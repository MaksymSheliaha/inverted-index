package com.example.invertedindex.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IndexResponse(
        boolean success,
        boolean indexed,
        Long documents,
        Long indexBuildTimeMs,
        Integer threadsUsed,
        String message
) {
    public static IndexResponse error(boolean indexed, String message) {
        return new IndexResponse(false, indexed, null, null, null, message);
    }

    public static IndexResponse successful(long documents, long indexBuildTimeMs, int threadsUsed) {
        return new IndexResponse(true, true, documents, indexBuildTimeMs, threadsUsed, null);
    }
}
