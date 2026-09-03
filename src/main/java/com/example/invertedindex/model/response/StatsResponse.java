package com.example.invertedindex.model.response;

public record StatsResponse(
        boolean indexed,
        long documents,
        long indexBuildTimeMs,
        int threadsUsed
) {
}
