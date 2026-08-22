package com.example.invertedindex.tools.map;

import java.util.List;
import java.util.Objects;

public class UnmodifiableMultivaluedMap<K, V> implements MultivaluedMap<K, V> {
    private final Node<K,List<V>>[] table;

    protected UnmodifiableMultivaluedMap(Node<K, List<V>>[] table) {
        this.table = new Node[table.length];

        for (int i = 0; i < table.length; i++) {
            Node<K, List<V>> node = table[i];
            Node<K, List<V>> previous = null;
            while (node != null) {
                Node<K, List<V>> copy = new Node<>(
                        node.key,
                        List.copyOf(node.value),
                        null
                );

                if (previous == null) {
                    this.table[i] = copy;
                } else {
                    previous.next = copy;
                }

                previous = copy;
                node = node.next;
            }
        }
    }

    @Override
    public List<V> get(K key) {
        int hash = key == null ? 0 : key.hashCode();
        int index = Math.floorMod(hash, table.length);

        var node = table[index];
        while (node != null) {
            if (Objects.equals(key, node.key)) {
                return node.value;
            }
            node = node.next;
        }
        return null;
    }

    @Override
    public void add(K key, V value) {
        throw new UnsupportedOperationException("UnmodifiableMultivaluedMap doesn't support add method");
    }

    @Override
    public boolean remove(K key) {
        throw new UnsupportedOperationException("UnmodifiableMultivaluedMap doesn't support remove method");
    }
}
