package com.example.invertedindex.service;

import com.example.invertedindex.index.AnalyzeUtils;
import com.example.invertedindex.model.index.Document;
import com.example.invertedindex.model.index.Posting;

import com.example.invertedindex.tools.executor.Executor;
import com.example.invertedindex.tools.map.MultivaluedConcurrentHashMap;
import com.example.invertedindex.tools.map.MultivaluedMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexService {

    private final DataProvider dataProvider;
    private final ObjectMapper objectMapper;

    private MultivaluedMap<String, Posting> invertedIndexSafe;

    public boolean indexDataset(Integer threadNum) {
        try {
            MultivaluedConcurrentHashMap<String, Posting> threadSafeInvertedIndex = new MultivaluedConcurrentHashMap<>();

            try (Executor executor = new Executor(threadNum)) {
                executor.start();
                dataProvider.getInputFiles().forEach(file ->
                    executor.submit(() -> processFile(file, threadSafeInvertedIndex)));
            }

            invertedIndexSafe = threadSafeInvertedIndex.getUnmodifiableMap();
            log.info("Indexing finished");
        } catch (Exception e) {
            log.error("Failed to index dataset", e);
            return false;
        }
        return true;
    }

    private void processFile(Path file, MultivaluedConcurrentHashMap<String, Posting> threadSafeInvertedIndex) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            var iterator = objectMapper.readerFor(Map.class).readValues(inputStream);

            while (iterator.hasNextValue()) {
                Document doc = new Document((Map<String, Object>) iterator.nextValue(), file);
                indexDoc(threadSafeInvertedIndex, doc);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to index file: " + file, e);
        }
    }

    @SneakyThrows
    private void indexDoc(MultivaluedConcurrentHashMap<String, Posting> index, Document doc) {
        String text = doc.getSource().get("description").toString();
        Map<String, Long> tokens = AnalyzeUtils.analyze(text)
                .stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        tokens.forEach((token, freq) ->
            index.add(token, new Posting(doc, freq)));
    }


    public List<Document> findDocs(String searchPhrase) {
        // todo: limit return results to top N documents
        var searchTerms = AnalyzeUtils.analyze(searchPhrase);
        return searchTerms.stream().flatMap(this::findForTerm)
                // todo: handle intersection of postings for multiple search terms
                // todo: marge posting with same document
                .collect(Collectors.groupingBy(Posting::document, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Document, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

    }

    private Stream<Posting> findForTerm(String searchTerm) {
        return invertedIndexSafe.get(searchTerm)
                .stream()
                .filter(Objects::nonNull);
    }
}
