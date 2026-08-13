package com.example.invertedindex.tools.map;

import java.util.List;

public interface MultivaluedMap<K, V> {
    List<V> get(K key);
    void add(K key, V value);
    boolean remove(K key);
}
