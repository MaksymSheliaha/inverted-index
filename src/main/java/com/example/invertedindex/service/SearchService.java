package com.example.invertedindex.service;

import com.example.invertedindex.index.AnalyzeUtils;
import com.example.invertedindex.model.Token;
import com.example.invertedindex.model.index.Document;
import com.example.invertedindex.model.index.Index;
import com.example.invertedindex.model.request.SearchRequest;
import com.example.invertedindex.model.response.ResponseDoc;
import com.example.invertedindex.model.response.SearchResponse;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.function.Consumer;
import com.example.invertedindex.model.response.SearchStreamMessage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    private final ObjectMapper objectMapper;
    private final OllamaService ollamaService;
    private final AtomicReference<Index> invertedIndex = new AtomicReference<>();
    private final TypeReference<Map<String, Object>> mapTypeRef = new TypeReference<>() {};

    public void setInvertedIndex(Index invertedIndex) {
        this.invertedIndex.set(invertedIndex);
    }

    public String getStatus() {
        return invertedIndex.get() == null ? "Index not loaded" : "Index loaded";
    }

    public void searchStream(SearchRequest searchRequest, Consumer<SearchStreamMessage> progressCallback) {
        throw new UnsupportedOperationException("Streaming search is not supported yet");
    }

    public SearchResponse findDocs(SearchRequest searchRequest) {
        if (invertedIndex.get() == null) return SearchResponse.EMPTY;

        Map<String, Object> timingContext = new HashMap<>();
        Map<String, Object> debugContext = new HashMap<>();
        debugContext.put("timingContext", timingContext);

        StopWatch stopWatch = new StopWatch();

        stopWatch.start("analyze");
        var searchTerms = getTokens(searchRequest);
        stopWatch.stop();

        stopWatch.start("search stage");
        var docs = findForTerms(searchTerms);
        stopWatch.stop();

        int numFound = docs.size();

        stopWatch.start("collect");
        var matches = sortAndPaginate(docs, numFound, searchRequest);
        var resultMap = readDocs(matches);

        var result = matches.stream()
                .map(match -> new ResponseDoc(resultMap.get(match.document()), match.score(), searchRequest.getMode().shouldExpand()))
                .toList();
        stopWatch.stop();

        Arrays.stream(stopWatch.getTaskInfo()).forEach(task ->
                timingContext.put(task.getTaskName(), task.getTimeMillis()));

        return SearchResponse.builder()
                .numFound(numFound)
                .page(searchRequest.getPage())
                .size(searchRequest.getSize())
                .docs(result)
                .searchTerms(searchTerms)
                .timings(searchRequest.isDebug() ? timingContext : Map.of())
                .build();
    }

    private List<Token> getTokens(SearchRequest searchRequest) {
        if (searchRequest.getMode().shouldExpand()) {
            return ollamaService.getTokens(searchRequest.getQuery());
        }
        return AnalyzeUtils.analyze(searchRequest.getQuery(), false)
                .stream().map(str -> new Token(str, Collections.emptyList())).toList();
    }

    private List<Match> sortAndPaginate(Map<Document, Double> mergedDocs, int numFound, SearchRequest searchRequest) {
        int expectedPage = searchRequest.getPage();
        int maxPage = (int) Math.ceil((double) numFound / searchRequest.getSize());
        if (expectedPage >= maxPage && expectedPage > 0) {
            log.warn("Expected page {} is greater than max page {}. Returning empty results.", expectedPage, maxPage);
            return List.of();
        }

        return mergedDocs.entrySet().stream()
                .map(entry -> new Match(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingDouble(Match::score).reversed())
                .skip((long) expectedPage * searchRequest.getSize())
                .limit(searchRequest.getSize())
                .toList();
    }

    private Map<Document, Double> findForTerms(List<Token> searchTerms) {
        return searchTerms.stream()
                .flatMap(this::findForToken)
                .collect(Collectors.groupingBy(Match::document, Collectors.summingDouble(Match::score)));
    }

    private Stream<Match> findForToken(Token token) {
        Stream<String> terms = Stream.concat(Stream.of(token.origToken()),
                CollectionUtils.isEmpty(token.synonyms()) ? Stream.empty() : token.synonyms().stream());

        return terms.flatMap(term -> Arrays.stream(invertedIndex.get().fieldIndexes())
                .flatMap(fieldIndex -> {
                    var docs = fieldIndex.postings().get(term);
                    if (CollectionUtils.isEmpty(docs)) return Stream.empty();
                    double boost = fieldIndex.field().boost();
                    return docs.stream().map(posting -> new Match(posting.document(), boost));
                }))
                .collect(Collectors.toMap(Match::document, Match::score, Double::max))
                .entrySet().stream()
                .map(entry -> new Match(entry.getKey(), entry.getValue()));
    }

    private Map<Document, Map<String, Object>> readDocs(List<Match> matches) {
        Map<Document, Map<String, Object>> result = new HashMap<>();
        Map<Path, List<Document>> grouped = matches.stream()
                .map(Match::document)
                .collect(Collectors.groupingBy(Document::getPath));

        for (Map.Entry<Path, List<Document>> entry : grouped.entrySet()) {
            try (RandomAccessFile raf = new RandomAccessFile(entry.getKey().toFile(), "r")) {
                for (var doc : entry.getValue()) {
                    raf.seek(doc.getLocation());
                    result.put(doc, objectMapper.readValue(raf.readLine(), mapTypeRef));
                }
            } catch (Exception e) {
                log.error("Error while reading file {}", entry.getKey(), e);
            }
        }
        return result;
    }

    private record Match(Document document, double score) {}
}
