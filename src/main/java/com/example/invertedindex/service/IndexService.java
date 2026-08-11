package com.example.invertedindex.service;

import com.example.invertedindex.index.AnalyzeUtils;
import com.example.invertedindex.model.index.Document;
import com.example.invertedindex.model.index.Posting;

import com.example.invertedindex.tools.map.MultivaluedConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexService {

    private final DataProvider dataProvider;
    private final ObjectMapper objectMapper;

    private Collection<Map<String, List<Posting>>> invertedIndex;
    private MultivaluedConcurrentHashMap<String, Posting> invertedIndexSafe;

    public boolean indexDataset(Integer threadNum) {
        try {
//            AtomicInteger counter = new AtomicInteger(0);
//            invertedIndex = IntStream.range(0, threadNum).parallel().mapToObj(this::indexSegment).toList();
            try(ExecutorService executor = Executors.newFixedThreadPool(threadNum)) {
                MultivaluedConcurrentHashMap<String, Posting> threadSafeInvertedIndex = new MultivaluedConcurrentHashMap<>();
                dataProvider.getInputFiles().forEach(file -> {
                    try (InputStream inputStream = Files.newInputStream(file)) {

                        var iterator = objectMapper.readerFor(Map.class).readValues(inputStream);

                        while (iterator.hasNextValue()) {
                            Document doc = new Document((Map<String, Object>) iterator.nextValue(), file);
                            executor.execute(() -> indexDoc(threadSafeInvertedIndex, doc));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to index file: " + file, e);
                    }
                });
                invertedIndexSafe = threadSafeInvertedIndex;
            }
        } catch (Exception e) {
            log.error("Failed to index dataset", e);
            return false;
        }
        return true;
    }

//    @SneakyThrows
//    private Map<String, List<Posting>> indexSegment(int segment) {
//        List<String> documents = TxtParser.parseShakespeareDocuments(file.getContentAsString(Charset.defaultCharset()));
//        return documents.stream().map(doc -> {
//            var tokens = AnalyzeUtils.analyze(doc).stream()
//                    .collect(Collectors.groupingBy(e->e , Collectors.counting()));
//            return Map.entry(tokens, doc);
//        }).flatMap(entry -> entry.getKey()
//                .entrySet().stream().map(freq -> Map.entry(freq.getKey(), new Posting(new Document(entry.getValue()), freq.getValue()))))
//                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
//    }

    @SneakyThrows
    private void indexDoc(Map<String, List<Posting>> index, Document doc) {
        String text = doc.getSource().get("description").toString();
        Map<String, Long> tokens = AnalyzeUtils.analyze(text)
                .stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        tokens.forEach((token, freq) -> {
            index.computeIfAbsent(token, k -> new ArrayList<>()).add(new Posting(doc, freq));
        });
    }

    @SneakyThrows
    private void indexDoc(MultivaluedConcurrentHashMap<String, Posting> index, Document doc) {
        String text = doc.getSource().get("description").toString();
        Map<String, Long> tokens = AnalyzeUtils.analyze(text)
                .stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        tokens.forEach((token, freq) -> {
            index.add(token, new Posting(doc, freq));
        });
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
