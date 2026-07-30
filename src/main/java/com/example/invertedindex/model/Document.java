package com.example.invertedindex.model;

import java.util.List;

public record Document(
        List<String> tokens,
        String doc
) {
}
