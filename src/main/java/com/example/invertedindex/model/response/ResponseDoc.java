package com.example.invertedindex.model.response;

import java.util.Map;

public record ResponseDoc (
        Long id,
        Map<String, Object> source,
        double score,
        boolean expandedResult
){}
