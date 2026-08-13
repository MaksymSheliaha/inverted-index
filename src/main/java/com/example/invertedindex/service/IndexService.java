package com.example.invertedindex.service;

import com.example.invertedindex.index.AnalyzeUtils;
import com.example.invertedindex.model.index.Document;
import com.example.invertedindex.model.index.Posting;

import com.example.invertedindex.tools.executor.Executor;
import com.example.invertedindex.tools.map.MultivaluedConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexService {

    private final DataProvider dataProvider;
    private final ObjectMapper objectMapper;
    private final SearchService searchService;

    private final ReentrantLock indexLock = new ReentrantLock();

    public boolean indexDataset(Integer threadNum) {
        if (!indexLock.tryLock()) {
            log.warn("Another indexing is in progress");
            return false;
        }
        try {
            MultivaluedConcurrentHashMap<String, Posting> threadSafeInvertedIndex = new MultivaluedConcurrentHashMap<>();
            try (Executor executor = new Executor(threadNum)) {
                executor.start();
                dataProvider.getInputFiles().forEach(file ->
                    executor.submit(() -> processFile(file, threadSafeInvertedIndex)));
            }

            searchService.setInvertedIndex(threadSafeInvertedIndex.getUnmodifiableMap());
            log.info("Indexing finished");
        } catch (Exception e) {
            log.error("Failed to index dataset", e);
            return false;
        } finally {
            indexLock.unlock();
        }
        return true;
    }

    private void processFile(Path file, MultivaluedConcurrentHashMap<String, Posting> threadSafeInvertedIndex) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            var iterator = objectMapper.readerFor(Map.class).readValues(inputStream);

            while (iterator.hasNextValue()) {
                var location = iterator.getCurrentLocation().getByteOffset() - 1;
                var content = (Map<String, Object>) iterator.nextValue();
                Document doc = new Document(location, file);
                indexDoc(threadSafeInvertedIndex, doc, content.get("description").toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to index file: " + file, e);
        }
    }

    @SneakyThrows
    private void indexDoc(MultivaluedConcurrentHashMap<String, Posting> index, Document doc, String content) {
        Map<String, Long> tokens = AnalyzeUtils.analyze(content)
                .stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        tokens.forEach((token, freq) ->
            index.add(token, new Posting(doc, freq)));
    }
}
