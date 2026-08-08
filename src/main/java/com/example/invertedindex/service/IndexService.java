package com.example.invertedindex.service;

import com.example.invertedindex.index.AnalyzeUtils;
import com.example.invertedindex.index.TxtParser;
import com.example.invertedindex.model.index.Document;
import com.example.invertedindex.model.index.Posting;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class IndexService {

    @Value("classpath:train_100.txt")
    Resource file;

    private List<Map<String, List<Posting>>> invertedIndex;
    public boolean indexDataset(Integer threadNum){
        try {
            invertedIndex = IntStream.range(0, threadNum).parallel().mapToObj(this::indexSegment).toList();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @SneakyThrows
    private Map<String, List<Posting>> indexSegment(int segment) {
        List<String> documents = TxtParser.parseShakespeareDocuments(file.getContentAsString(Charset.defaultCharset()));
        return documents.stream().map(doc -> {
            var tokens = AnalyzeUtils.analyze(doc).stream()
                    .collect(Collectors.groupingBy(e->e , Collectors.counting()));
            return Map.entry(tokens, doc);
        }).flatMap(entry -> entry.getKey()
                .entrySet().stream().map(freq -> Map.entry(freq.getKey(), new Posting(new Document(entry.getValue()), freq.getValue()))))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    public List<Document> findDocs(String searchPhrase){
        var searchTerms = AnalyzeUtils.analyze(searchPhrase);
        return searchTerms.stream().map(this::findForTerm)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(Posting::document, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Document, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

    }

    private Set<Posting> findForTerm(String searchTerm){
        return invertedIndex.parallelStream().map(termDic -> termDic.get(searchTerm))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
