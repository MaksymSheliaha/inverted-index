package com.example.invertedindex.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Token(
        String origToken,
        List<String> synonyms
) {
}
