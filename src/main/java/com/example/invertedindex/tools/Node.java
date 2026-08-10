package com.example.invertedindex.tools;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class Node<K, V>{
    final K key;
    volatile V value;
    volatile Node<K, V> next;
}
