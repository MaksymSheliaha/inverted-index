package com.example.invertedindex.service;

import com.example.invertedindex.model.Document;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
public class IndexService {
    private List<Map<String, List<Document>>> invertedIndex;
    public boolean indexDataset(Integer threadNum){
        try {
            invertedIndex = IntStream.range(0, threadNum).parallel().mapToObj(this::indexSegment).toList();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private Map<String, List<Document>> indexSegment(int segment) {
        return null;
    }

    public List<Document> findDocs(String searchTerm){
        return invertedIndex.parallelStream().map(termDic -> termDic.get(searchTerm))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
    }
}
