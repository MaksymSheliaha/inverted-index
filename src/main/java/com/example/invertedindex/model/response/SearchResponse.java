package com.example.invertedindex.model.response;

import com.example.invertedindex.model.Token;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SearchResponse {
    private int numFound;
    private int page;
    private int size;
    private List<ResponseDoc> docs;
    private List<Token> searchTerms;
    private Map<String, Object> timings;

    public static SearchResponse EMPTY = new SearchResponse(0, 0, 0, List.of(), List.of(), Map.of());
}
