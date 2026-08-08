package com.example.invertedindex.service;

import com.example.invertedindex.index.AnalyzeUtils;
import com.example.invertedindex.model.index.Document;
import com.example.invertedindex.model.index.Posting;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class IndexService {

    private final DataProvider dataProvider;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    private Collection<Map<String, List<Posting>>> invertedIndex;

    public boolean indexDataset(Integer threadNum){
        try {
//            AtomicInteger counter = new AtomicInteger(0);
//            invertedIndex = IntStream.range(0, threadNum).parallel().mapToObj(this::indexSegment).toList();
            Map<Thread, Map<String, List<Posting>>> threadSafeInvertedIndex = new ConcurrentHashMap<>();
            dataProvider.getInputFiles().forEach(file -> {
                try {
                    executor.submit(() -> {
                        var index = threadSafeInvertedIndex.computeIfAbsent(Thread.currentThread(), k -> new HashMap<>());
                        indexDoc(index, file);
                    });
                } catch (Exception e) {
                    throw new RuntimeException("Failed to index file: " + file, e);
                }
            });

            invertedIndex = threadSafeInvertedIndex.values().stream().toList();
        } catch (Exception e) {
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
    private void indexDoc(Map<String, List<Posting>> index, Path file) {
        Document doc = new Document(file.getFileName().toString(), file);
        var document = objectMapper.readTree(file.toFile());
        Map<String, Long> tokens = AnalyzeUtils.analyze(document.get("text").asText())
                .stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        tokens.forEach((token, freq) -> {
            index.computeIfAbsent(token, k -> new ArrayList<>()).add(new Posting(doc, freq));
        });
    }


    public List<Document> findDocs(String searchPhrase){
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

    private Stream<Posting> findForTerm(String searchTerm){
        return invertedIndex.parallelStream().map(termDic -> termDic.get(searchTerm))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream);
    }
}
