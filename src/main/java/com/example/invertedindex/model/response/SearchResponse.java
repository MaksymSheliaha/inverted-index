package com.example.invertedindex.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class SearchResponse {
    private int numFound;
    private int page;
    private int size;
    private List<Map<String, Object>> docs;

    public static SearchResponse EMPTY = new SearchResponse(0, 0, 0, List.of());
}
