package com.example.invertedindex.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SearchableFields {
    // todo: can be externalized to json/xml file
    TITLE("title", SearchableFieldType.TEXT, 100.0f),
    DESCRIPTION("description", SearchableFieldType.LIST, 10.0f),
    FEATURES("features", SearchableFieldType.LIST, 50.0f),
    CATEGORIES("categories", SearchableFieldType.LIST, 200.0f),
    MAIN_CATEGORY("main_category", SearchableFieldType.TEXT, 300f),
    STORE("store", SearchableFieldType.TEXT, 1.2f);

    private final String fieldName;
    private final SearchableFieldType type;
    public final float boost;
}

