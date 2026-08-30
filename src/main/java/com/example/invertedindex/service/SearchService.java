package com.example.invertedindex.service;

import com.example.invertedindex.index.AnalyzeUtils;
import com.example.invertedindex.model.index.Document;
import com.example.invertedindex.model.index.Field;
import com.example.invertedindex.model.index.Index;
import com.example.invertedindex.model.request.SearchRequest;
import com.example.invertedindex.model.response.ResponseDoc;
import com.example.invertedindex.model.response.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    private static final int MIN_NUM_FOUND_THRESHOLD = 10;

    private final ObjectMapper objectMapper;
    private final OllamaService ollamaService;
    private final AtomicReference<Index> invertedIndex = new AtomicReference<>();

    public void setInvertedIndex(Index invertedIndex) {
        this.invertedIndex.set(invertedIndex);
    }

    public SearchResponse findDocs(SearchRequest searchRequest) {
        if(invertedIndex.get() == null) return SearchResponse.EMPTY;
        Map timingContext = new HashMap<>();
        Map debugContext = new HashMap<>();
        debugContext.put("timingContext", timingContext);

        StopWatch analizeStopWatch = new StopWatch();
        analizeStopWatch.start("analyze");
        var searchTerms = AnalyzeUtils.analyze(searchRequest.getSearchTerm(), false);
        analizeStopWatch.stop();

        StopWatch firstStageStopWatch = new StopWatch();
        firstStageStopWatch.start("firstStage");
        var docs = findForTerms(searchTerms);
        firstStageStopWatch.stop();

        var expandedDocs = expandResults(docs, searchRequest, debugContext);
        int numFound = docs.size() + expandedDocs.size();

        StopWatch collectStopWatch = new StopWatch();
        collectStopWatch.start("collect");
        var matches = mergeMatches(docs, expandedDocs, numFound, searchRequest);

        var resultMap = readDocs(matches);

        var result = matches.stream()
                .map(match -> new ResponseDoc(
                            resultMap.get(match.document()), match.score(),
                            expandedDocs.containsKey(match.document())
                        )
                )
                .toList();

        collectStopWatch.stop();

        timingContext.put("analyze", analizeStopWatch.getTotalTimeMillis());
        timingContext.put("first stage", firstStageStopWatch.getTotalTimeMillis());
        timingContext.put("collect", collectStopWatch.getTotalTimeMillis());

        return SearchResponse.builder()
                .numFound(numFound)
                .page(searchRequest.getPage())
                .size(searchRequest.getSize())
                .docs(result)
                .searchTerms(searchTerms)
                .extendSearchTerms((List<String>)debugContext.get("expandedSearchTerms"))
                .timings(searchRequest.isDebug() ? timingContext : Map.of())
                .build();
    }

    private Map<Document, Double> expandResults(Map<Document, Double> docs, SearchRequest searchRequest, Map debugContext) {
        if (shouldExpandQuery(docs, searchRequest)) {
            Map timingContext = (Map) debugContext.get("timingContext");
            StopWatch expandStopWatch = new StopWatch();
            expandStopWatch.start("expand");
            var expandedTerms = ollamaService.expandQuery(searchRequest.getSearchTerm());
            expandStopWatch.stop();

            log.info("Expanded query terms: {}", expandedTerms);
            debugContext.put("expandedSearchTerms", expandedTerms);

            StopWatch expandedStageStopWatch = new StopWatch();
            expandedStageStopWatch.start("expandStage");
            var result = findForTerms(expandedTerms);
            expandedStageStopWatch.stop();

            timingContext.put("expand", expandStopWatch.getTotalTimeMillis());
            timingContext.put("expanded search", expandStopWatch.getTotalTimeMillis());

            return result;
        }

        return Map.of();
    }

    private List<Match> mergeMatches(Map<Document, Double> docs, Map<Document, Double> expandedDocs,
                                     int numFound, SearchRequest searchRequest) {
        int expectedPage = searchRequest.getPage();
        int maxPage = (int) Math.ceil((double) numFound / searchRequest.getSize());
        if (expectedPage >= maxPage) {
            log.warn("Expected page {} is greater than max page {}. Returning empty results.", expectedPage, maxPage);
            return List.of();
        }

        long offset = (long) expectedPage * searchRequest.getSize();

        var docStream = docs.entrySet().stream()
                .map(entry -> new Match(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingDouble(Match::score).reversed());

        var extendedDocStream = expandedDocs.entrySet().stream()
                .map(entry -> new Match(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingDouble(Match::score).reversed());

        return Stream.concat(docStream, extendedDocStream)
                .skip(offset)
                .limit(searchRequest.getSize())
                .toList();
    }

    private Map<Document, Double> findForTerms(List<String> searchTerms) {
        return searchTerms.stream().flatMap(this::findForTerm)
                .collect(Collectors.groupingBy(Match::document, Collectors.summingDouble(Match::score)));
    }

    private Stream<Match> findForTerm(String searchTerm) {
        return Arrays.stream(invertedIndex.get().fieldIndexes())
                .flatMap(fieldIndex -> {
                    var docs = fieldIndex.postings().get(searchTerm);
                    if (CollectionUtils.isEmpty(docs)) {
                        return Stream.empty();
                    }
                    return docs.stream()
                            .map(posting -> new FieldMatch(posting.document(), fieldIndex.field()));
                })
                .collect(Collectors.groupingBy(FieldMatch::document, Collectors.mapping(FieldMatch::field, Collectors.toList())))
                .entrySet()
                .stream()
                .map(entry -> new Match(entry.getKey(), getScore(entry.getValue())));
    }

    private Map<Document, Map<String, Object>> readDocs(List<Match> matches) {
        Map<Document, Map<String, Object>> result = new HashMap<>();
        Map<Path, List<Document>> grouped = matches.stream()
                .map(Match::document)
                .collect(Collectors.groupingBy(Document::getPath));

        for(Map.Entry<Path, List<Document>> entry: grouped.entrySet()) {
            var file = entry.getKey();
            var documents = entry.getValue();
            try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
                for(var doc: documents) {
                    raf.seek(doc.getLocation());
                    String jsonLine = raf.readLine();
                    result.put(doc, objectMapper.readValue(jsonLine, Map.class));
                }
            } catch (Exception e) {
                log.error("Error while reading file ", e);
            }
        }

        return result;
    }

    private double getScore(List<Field> fields) {
        return fields.stream().mapToDouble(Field::boost).max().orElse(0.0);
    }

    private boolean shouldExpandQuery(Map matches, SearchRequest request) {
        int threshold = Math.max(MIN_NUM_FOUND_THRESHOLD, request.getSize());
        return matches.size() < threshold;
    }

    private record Match(Document document, double score) {}
    private record FieldMatch(Document document, Field field) {}
}
