package com.example.invertedindex.tools.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class MultivaluedConcurrentHashMap<K, V> implements MultivaluedMap<K, V> {
    public static final int DEFAULT_CAPACITY = 64;
    public static final int DEFAULT_SEGMENT_COUNT = 16;
    public static final double DEFAULT_LOAD_FACTOR = 0.75;

    private volatile Node<K, List<V>>[] table;
    private final Lock[] locks;
    private final AtomicInteger size;
    private final double loadFactor;

    public MultivaluedConcurrentHashMap() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public MultivaluedConcurrentHashMap(int capacity, double loadFactor) {
        this.table = new Node[capacity];
        this.size = new AtomicInteger(0);
        this.loadFactor = loadFactor;
        int segmentCount = calculateSegmentCount(capacity);
        this.locks = new ReentrantLock[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            this.locks[i] = new ReentrantLock();
        }
    }

    private static int calculateSegmentCount(int capacity) {
        int target = Math.min(DEFAULT_SEGMENT_COUNT, capacity);

        for (int segments = target; segments >= 1; segments--) {
            if (capacity % segments == 0) {
                return segments;
            }
        }

        return 1;
    }

    /**
     * Returns list of values
     **/
    @Override
    public List<V> get(K key) {
        int hash = hash(key);
        int lockIndex = lockIndex(hash);

        locks[lockIndex].lock();
        try {
            int index = tableIndex(hash);
            var node = table[index];
            while (node != null) {
                if (Objects.equals(key, node.key)) {
                    return List.copyOf(node.value);
                }
                node = node.next;
            }

        } finally {
            locks[lockIndex].unlock();
        }
        return null;
    }

    @Override
    public void add(K key, V value) {
        int hash = hash(key);
        int lockIndex = lockIndex(hash);

        locks[lockIndex].lock();
        try {
            int index = tableIndex(hash);
            Node<K, List<V>> node = table[index];

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
        int hash = hash(key);
        int index = Math.floorMod(hash, table.length);
        table[index] = new Node<>(key, value, table[index]);
    }

    @Override
    public boolean remove(K key) {
        int hash = hash(key);
        int lockIndex = lockIndex(hash);

        locks[lockIndex].lock();
        try {
            int index = tableIndex(hash);
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

    private int hash(Object key) {
        return key == null ? 0 : key.hashCode();
    }

    private int lockIndex(int hash) {
        return Math.floorMod(hash, locks.length);
    }

    private int tableIndex(int hash) {
        return Math.floorMod(hash, table.length);
    }

    @Override
    public UnmodifiableMultivaluedMap<K, V> getUnmodifiableMap(){
        for(var lock: locks) {
            lock.lock();
        }
        try{
            return new UnmodifiableMultivaluedMap<>(table);
        } finally {
            for(var lock: locks) {
                lock.unlock();
            }
        }
    }
}
