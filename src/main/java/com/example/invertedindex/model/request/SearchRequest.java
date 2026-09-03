package com.example.invertedindex.model.request;

import lombok.Data;

@Data
public class SearchRequest {
    private String query;
    private SearchMode mode = SearchMode.KEYWORD;
    private Integer page = 0;
    private Integer size = 10;
    private boolean debug = false;
}
