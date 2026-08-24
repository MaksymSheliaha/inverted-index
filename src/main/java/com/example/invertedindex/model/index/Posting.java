package com.example.invertedindex.model.index;

import com.example.invertedindex.constants.SearchableFields;

public record Posting(Document document, long freq) {
}
