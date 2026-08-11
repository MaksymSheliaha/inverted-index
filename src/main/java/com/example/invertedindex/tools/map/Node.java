package com.example.invertedindex.tools.map;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class Node<K, V>{
    final K key;
    volatile V value;
    volatile Node<K, V> next;
}
