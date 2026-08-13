package com.example.invertedindex.service;

import com.example.invertedindex.index.AnalyzeUtils;
import com.example.invertedindex.model.index.Document;
import com.example.invertedindex.model.index.Posting;
import com.example.invertedindex.model.request.SearchRequest;
import com.example.invertedindex.model.response.SearchResponse;
import com.example.invertedindex.tools.map.MultivaluedMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    private final ObjectMapper objectMapper;
    private final AtomicReference<MultivaluedMap<String, Posting>> invertedIndex = new AtomicReference<>();

    public void setInvertedIndex(MultivaluedMap<String, Posting> invertedIndex) {
        this.invertedIndex.set(invertedIndex);
    }

    public SearchResponse findDocs(SearchRequest searchRequest) {
        if(invertedIndex.get() == null) return SearchResponse.EMPTY;
        // todo: limit return results to top N documents
        var searchTerms = AnalyzeUtils.analyze(searchRequest.getSearchTerm());
        var docs = searchTerms.stream().flatMap(this::findForTerm)
                // todo: handle intersection of postings for multiple search terms
                // todo: marge posting with same document
                .collect(Collectors.groupingBy(Posting::document, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Document, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .skip((long) searchRequest.getPage() * searchRequest.getSize())
                .limit(searchRequest.getSize())
                .toList();

        var resultMap = readDocs(docs);

        var result =  docs.stream()
                .map(resultMap::get)
                .toList();

        return new SearchResponse(result.size(), searchRequest.getPage(), searchRequest.getSize(), result);

    }

    private Stream<Posting> findForTerm(String searchTerm) {
        return invertedIndex.get().get(searchTerm)
                .stream()
                .filter(Objects::nonNull);
    }

    private Map<Document, Map<String, Object>> readDocs(List<Document> docs) {
        Map<Document, Map<String, Object>> result = new HashMap<>();
        Map<Path, List<Document>> grouped = docs.stream()
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
}
