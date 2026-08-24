package com.example.invertedindex.model.request;

import lombok.Data;

@Data
public class SearchRequest {
    private String searchTerm;
    private Integer page = 0;
    private Integer size = 10;
    private boolean debug = false;
}
