package com.example.invertedindex.model.index;

import com.example.invertedindex.constants.SearchableFields;
import com.example.invertedindex.tools.map.MultivaluedConcurrentHashMap;
import com.example.invertedindex.tools.map.MultivaluedMap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record Index(FieldIndex[] fieldIndexes) {

    public Index toReadIndex() {
        var readFieldIndexes = Arrays.stream(fieldIndexes)
                .map(fieldIndex -> new FieldIndex(fieldIndex.field(), fieldIndex.postings().getUnmodifiableMap()))
                .toArray(FieldIndex[]::new);

        return new Index(readFieldIndexes);
    }

    public static Index initWriteIndex(List<Field> fields) {
        var fieldIndexes = fields.stream()
                .map(entry -> new FieldIndex(entry, new MultivaluedConcurrentHashMap<>()))
                .toList();

        return new Index(fieldIndexes.toArray(new FieldIndex[0]));
    }

    public record FieldIndex(
            Field field,
            MultivaluedMap<String, Posting> postings
    ) {}
}

