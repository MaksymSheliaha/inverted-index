package com.example.invertedindex.tools.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class MultivaluedConcurrentHashMap<K, V> {
    public static final int DEFAULT_CAPACITY = 64;
    public static final int DEFAULT_SEGMENT_COUNT = 16;
    public static final double DEFAULT_LOAD_FACTOR = 0.75;

    private volatile Node<K, List<V>>[] table;
    private final Lock[] locks;
    private final AtomicInteger size;
    private final double loadFactor;

    public MultivaluedConcurrentHashMap() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_SEGMENT_COUNT);
    }

    public MultivaluedConcurrentHashMap(int capacity, double loadFactor, int segmentCount) {
        this.table = new Node[capacity];
        this.size = new AtomicInteger(0);
        this.loadFactor = loadFactor;
        this.locks = new ReentrantLock[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            this.locks[i] = new ReentrantLock();
        }
    }

    /**
     * Returns unsafe read-only list of values
     **/
    public List<V> get(K key) {
        int hash = key == null ? 0 : key.hashCode();
        int index = Math.abs(hash) % table.length;
        var node = table[index];
        while (node != null) {
            if (Objects.equals(key, node.key)) {
                return Collections.unmodifiableList(node.value);
            }
            node = node.next;
        }
        return null;
    }

    public void add(K key, V value) {
        int hash = key == null ? 0 : key.hashCode();
        int lockIndex = Math.abs(hash) % locks.length;

        locks[lockIndex].lock();
        try {
            int index = Math.abs(hash) % table.length;
            var node = table[index];

            while (node != null) {
                if (Objects.equals(key, node.key)) {
                    node.value.add(value);
                    return;
                }
                node = node.next;
            }
            List<V> list = new ArrayList<>();
            list.add(value);
            table[index] = new Node<>(key, list, table[index]);

        } finally {
            locks[lockIndex].unlock();
        }

        int currentSize = size.incrementAndGet();
        if ((double) currentSize / table.length > loadFactor) {
            resize();
        }
    }

    private void put(K key, List<V> value, Node<K, List<V>>[] table) {
        int hash = key == null ? 0 : key.hashCode();
        int index = Math.abs(hash) % table.length;
        table[index] = new Node<>(key, value, table[index]);
    }

    public boolean remove(K key) {
        int hash = key == null ? 0 : key.hashCode();
        int lockIndex = Math.abs(hash) % locks.length;

        locks[lockIndex].lock();
        try {
            int index = Math.abs(hash) % table.length;
            var node = table[index];
            Node<K, List<V>> prev = null;
            while (node != null) {
                if (Objects.equals(key, node.key)) {
                    if (prev == null) {
                        table[index] = node.next;
                    } else {
                        prev.next = node.next;
                    }
                    size.decrementAndGet();
                    return true;
                }
                prev = node;
                node = node.next;
            }
            return false;
        } finally {
            locks[lockIndex].unlock();
        }
    }

    private void resize() {
        for(var lock: locks) {
            lock.lock();
        }
        try {
            if ((double) size.get() / table.length > loadFactor) {
                int newCapacity = table.length * 2;
                Node<K, List<V>>[] newTable = new Node[newCapacity];
                forEach((k, v) -> put(k, v, newTable));
                table = newTable;            }
        } finally {
            for(var lock: locks) {
                lock.unlock();
            }
        }
    }

    private void forEach(BiConsumer<K, List<V>> consumer) {
        for (Node<K, List<V>> node : table) {
            while (node != null) {
                consumer.accept(node.key, node.value);
                node = node.next;
            }
        }
    }
}
