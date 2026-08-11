package com.example.invertedindex.tools.map;

import java.util.List;
import java.util.Objects;

public class UnmodifiableMultivaluedMap<K, V> {
    private Node<K,List<V>>[] table;

    protected UnmodifiableMultivaluedMap(Node<K, List<V>>[] table) {
        this.table = new Node[table.length];

        for(int i = 0; i < table.length; i++) {
            var node = table[i];
            if(node == null) continue;
            var copy = new Node<>(node.key, List.copyOf(node.value), null);
            node = node.next;
            while(node != null) {
                copy.next = new Node<>(node.key, List.copyOf(node.value), null);
                node = node.next;
            }
        }
    }

    public List<V> get(K key) {
        int hash = key == null ? 0 : key.hashCode();
        int index = Math.abs(hash) % table.length;
        var node = table[index];
        while (node != null) {
            if (Objects.equals(key, node.key)) {
                return node.value;
            }
            node = node.next;
        }
        return null;
    }
}
