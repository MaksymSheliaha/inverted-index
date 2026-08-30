package com.example.invertedindex.model.index;

import com.example.invertedindex.constants.SearchableFieldType;

public record Field(
        String name,
        SearchableFieldType type,
        double boost
) {
}
