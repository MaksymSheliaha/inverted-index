package com.example.invertedindex.service;

import com.example.invertedindex.index.TxtParser;
import com.example.invertedindex.model.Document;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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

    private List<Map<String, List<Document>>> invertedIndex;
    public boolean indexDataset(Integer threadNum){
        try {
            invertedIndex = IntStream.range(0, threadNum).parallel().mapToObj(this::indexSegment).toList();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @SneakyThrows
    private Map<String, List<Document>> indexSegment(int segment) {
        List<String> documents = TxtParser.parseShakespeareDocuments(file.getContentAsString(Charset.defaultCharset()));
        return documents.stream().map(doc -> {
            List<String> tokens = Arrays.asList(doc.split("\\s+"));
            return new Document(tokens, doc);
        }).flatMap(entry -> entry.tokens().stream().map(token -> Map.entry(token, entry)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    public List<Document> findDocs(String searchTerm){
        return invertedIndex.parallelStream().map(termDic -> termDic.get(searchTerm))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
    }
}
