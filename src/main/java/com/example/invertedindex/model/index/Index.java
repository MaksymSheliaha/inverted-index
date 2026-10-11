package com.example.invertedindex.model.index;

import com.example.invertedindex.tools.map.MultivaluedConcurrentHashMap;
import com.example.invertedindex.tools.map.MultivaluedMap;
import com.example.invertedindex.tools.map.ConcurrentHashMap;

import java.util.Arrays;
import java.util.List;

public record Index(FieldIndex[] fieldIndexes, ConcurrentHashMap<Long, Document> documents) {

    public Index toReadIndex() {
        var readFieldIndexes = Arrays.stream(fieldIndexes)
                .map(fieldIndex -> new FieldIndex(fieldIndex.field(), fieldIndex.postings().getUnmodifiableMap()))
                .toArray(FieldIndex[]::new);

        return new Index(readFieldIndexes, documents);
    }

    public static Index initWriteIndex(List<Field> fields) {
        var fieldIndexes = fields.stream()
                .map(entry -> new FieldIndex(entry, new MultivaluedConcurrentHashMap<>()))
                .toList();

        return new Index(fieldIndexes.toArray(new FieldIndex[0]), new ConcurrentHashMap<>(1.f)); // Load factor 1.0 because of autoincrement id strategy
    }

    public record FieldIndex(
            Field field,
            MultivaluedMap<String, Posting> postings
    ) {}
}