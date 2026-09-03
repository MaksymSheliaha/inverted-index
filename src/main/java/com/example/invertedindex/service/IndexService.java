package com.example.invertedindex.service;

import com.example.invertedindex.constants.SearchableFields;
import com.example.invertedindex.index.AnalyzeUtils;
import com.example.invertedindex.model.index.Document;
import com.example.invertedindex.model.index.Field;
import com.example.invertedindex.model.index.Index;
import com.example.invertedindex.model.index.Posting;

import com.example.invertedindex.tools.executor.Executor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
    private final AtomicInteger documentCount = new AtomicInteger(0);
    private final AtomicBoolean indexed = new AtomicBoolean(false);
    private final AtomicLong indexBuildTimeMs = new AtomicLong(0);
    private final AtomicInteger threadsUsed = new AtomicInteger(0);

    public int getDocumentCount() {
        return documentCount.get();
    }

    public boolean isIndexed() {
        return indexed.get();
    }

    public long getIndexBuildTimeMs() {
        return indexBuildTimeMs.get();
    }

    public int getThreadsUsed() {
        return threadsUsed.get();
    }

    public boolean indexDataset(Integer threadNum) {
        if (!indexLock.tryLock()) {
            log.warn("Another indexing is in progress");
            return false;
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        threadsUsed.set(threadNum);
        try {
            documentCount.set(0);
            var index = Index.initWriteIndex(getFields());
            try (Executor executor = new Executor(threadNum)) {
                executor.start();
                dataProvider.getInputFiles().forEach(file ->
                    executor.submit(() -> processFile(file, index)));
            }

            searchService.setInvertedIndex(index.toReadIndex());
            log.info("Indexing finished");
            stopWatch.stop();
            indexBuildTimeMs.set(stopWatch.getTotalTimeMillis());
            indexed.set(true);
        } catch (Exception e) {
            log.error("Failed to index dataset", e);
            return false;
        } finally {
            indexLock.unlock();
        }
        return true;
    }

    private void processFile(Path file, Index index) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            var iterator = objectMapper.readerFor(Map.class).readValues(inputStream);

            while (iterator.hasNextValue()) {
                var location = iterator.getCurrentLocation().getByteOffset() - 1;
                var content = (Map<String, Object>) iterator.nextValue();
                Document doc = new Document(location, file);
                indexDoc(index, doc, content);
                documentCount.incrementAndGet();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to index file: " + file, e);
        }
    }

    @SneakyThrows
    private void indexDoc(Index index, Document doc, Map<String, Object> content) {
        for(var fieldIndex: index.fieldIndexes()) {
            var field = fieldIndex.field();
            var postings = fieldIndex.postings();
            Object fieldValue = content.get(field.name());
            if (fieldValue != null) {
                Map<String, Long> tokens = AnalyzeUtils.analyzeField(fieldValue, field.type(), false)
                        .stream()
                        .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
                for (Map.Entry<String, Long> entry : tokens.entrySet()) {
                    String token = entry.getKey();
                    Long count = entry.getValue();
                    postings.add(token, new Posting(doc, count));
                }
            }
        }
    }

    private List<Field> getFields() {
        List<Field> fields = new ArrayList<>();
        for (SearchableFields field : SearchableFields.values()) {
            fields.add(new Field(field.getFieldName(), field.getType(), field.getBoost()));
        }
        return fields;
    }
}