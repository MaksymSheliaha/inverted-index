package com.example.invertedindex.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SearchMode {
    KEYWORD(false),
    EXPANDED(true);

    private final boolean expand;

    SearchMode(boolean expand) {
        this.expand = expand;
    }

    public boolean shouldExpand() {
        return expand;
    }

    @JsonCreator
    public static SearchMode fromString(String value) {
        if (value == null) {
            return KEYWORD;
        }
        for (SearchMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return KEYWORD;
    }
}
