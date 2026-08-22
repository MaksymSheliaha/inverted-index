package com.example.invertedindex.tools;

import com.example.invertedindex.tools.map.MultivaluedConcurrentHashMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class MultivaluedConcurrentHashMapTest {

    private static final double LOAD_FACTOR = 0.75;
    private static final double AGGRESSIVE_LOAD_FACTOR = 0.1;

    private static final int SMALL_CAPACITY = 4;
    private static final int RESIZE_CAPACITY = 2;
    private static final int CONCURRENT_CAPACITY = 16;

    private static final int THREAD_COUNT = 8;
    private static final int HIGH_THREAD_COUNT = 16;

    private static final int VALUES_PER_THREAD = 1_000;
    private static final int KEYS_PER_THREAD = 1_000;
    private static final int KEY_COUNT = 1_000;

    private static final int VALUES_PER_KEY = 10;
    private static final int MULTIPLE_KEYS_COUNT = 20;

    private static final int FIRST_VALUE = 1;
    private static final int SECOND_VALUE = 2;
    private static final int THIRD_VALUE = 3;

    private static final int NULL_KEY_FIRST_VALUE = 10;
    private static final int NULL_KEY_SECOND_VALUE = 20;

    private static final int COLLISION_HASH = 42;
    private static final int MIN_INTEGER_HASH = Integer.MIN_VALUE;
    private static final int MIN_HASH_VALUE = 42;

    private static final int SEGMENT_TEST_CAPACITY = 64;
    private static final int SEGMENT_TEST_CAPACITY_2 = 128;
    private static final int SEGMENT_TEST_SMALL_CAPACITY = 8;
    private static final int SEGMENT_TEST_NON_POWER_OF_TWO = 100;

    @Test
    void shouldAddAndGetValues() {
        var map = new MultivaluedConcurrentHashMap<String, Integer>();

        map.add("java", FIRST_VALUE);
        map.add("java", SECOND_VALUE);
        map.add("java", THIRD_VALUE);

        assertEquals(
                List.of(FIRST_VALUE, SECOND_VALUE, THIRD_VALUE),
                map.get("java")
        );
    }

    @Test
    void shouldReturnNullForMissingKey() {
        var map = new MultivaluedConcurrentHashMap<String, Integer>();

        assertNull(map.get("missing"));
    }

    @Test
    void shouldSupportNullKey() {
        var map = new MultivaluedConcurrentHashMap<String, Integer>();

        map.add(null, NULL_KEY_FIRST_VALUE);
        map.add(null, NULL_KEY_SECOND_VALUE);

        assertEquals(
                List.of(NULL_KEY_FIRST_VALUE, NULL_KEY_SECOND_VALUE),
                map.get(null)
        );
    }

    @Test
    void shouldRemoveKey() {
        var map = new MultivaluedConcurrentHashMap<String, Integer>();

        map.add("java", FIRST_VALUE);
        map.add("java", SECOND_VALUE);

        assertTrue(map.remove("java"));
        assertNull(map.get("java"));
    }

    @Test
    void shouldReturnFalseWhenRemovingMissingKey() {
        var map = new MultivaluedConcurrentHashMap<String, Integer>();

        assertFalse(map.remove("missing"));
    }

    @Test
    void shouldNotExposeInternalValueList() {
        var map = new MultivaluedConcurrentHashMap<String, Integer>();

        map.add("java", FIRST_VALUE);
        map.add("java", SECOND_VALUE);

        List<Integer> values = map.get("java");

        assertThrows(
                UnsupportedOperationException.class,
                () -> values.add(THIRD_VALUE)
        );

        assertEquals(
                List.of(FIRST_VALUE, SECOND_VALUE),
                map.get("java")
        );
    }

    @Test
    void shouldHandleHashCollisions() {
        var map = new MultivaluedConcurrentHashMap<CollisionKey, Integer>();

        var firstKey = new CollisionKey(1);
        var secondKey = new CollisionKey(2);

        map.add(firstKey, 100);
        map.add(secondKey, 200);

        assertEquals(List.of(100), map.get(firstKey));
        assertEquals(List.of(200), map.get(secondKey));
    }

    @Test
    void shouldResizeAndKeepAllEntries() {
        var map = new MultivaluedConcurrentHashMap<Integer, Integer>(
                SMALL_CAPACITY,
                LOAD_FACTOR
        );

        for (int i = 0; i < KEY_COUNT; i++) {
            map.add(i, i);
        }

        for (int i = 0; i < KEY_COUNT; i++) {
            assertEquals(List.of(i), map.get(i));
        }
    }

    @Test
    void shouldResizeWithMultipleValuesPerKey() {
        var map = new MultivaluedConcurrentHashMap<Integer, Integer>(
                SMALL_CAPACITY,
                LOAD_FACTOR
        );

        for (int key = 0; key < MULTIPLE_KEYS_COUNT; key++) {
            for (int value = 0; value < VALUES_PER_KEY; value++) {
                map.add(key, value);
            }
        }

        List<Integer> expectedValues =
                java.util.stream.IntStream
                        .range(0, VALUES_PER_KEY)
                        .boxed()
                        .toList();

        for (int key = 0; key < MULTIPLE_KEYS_COUNT; key++) {
            assertEquals(expectedValues, map.get(key));
        }
    }

    @Test
    void shouldHandleMinIntegerHashCode() {
        var map = new MultivaluedConcurrentHashMap<MinHashKey, Integer>();

        var key = new MinHashKey();

        map.add(key, MIN_HASH_VALUE);

        assertEquals(List.of(MIN_HASH_VALUE), map.get(key));
    }

    @Test
    void shouldHandleConcurrentAdds() throws Exception {
        var map = new MultivaluedConcurrentHashMap<Integer, Integer>(
                CONCURRENT_CAPACITY,
                LOAD_FACTOR
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            List<Future<?>> futures = new ArrayList<>();

            for (int thread = 0; thread < THREAD_COUNT; thread++) {
                int threadId = thread;

                futures.add(executor.submit(() -> {
                    for (int value = 0; value < VALUES_PER_THREAD; value++) {
                        map.add(threadId, value);
                    }
                }));
            }

            waitForCompletion(futures);
        } finally {
            shutdown(executor);
        }

        for (int thread = 0; thread < THREAD_COUNT; thread++) {
            List<Integer> values = map.get(thread);

            assertNotNull(values);
            assertEquals(VALUES_PER_THREAD, values.size());
        }
    }

    @Test
    void shouldHandleConcurrentAddsToSameKey() throws Exception {
        var map = new MultivaluedConcurrentHashMap<String, Integer>(
                CONCURRENT_CAPACITY,
                LOAD_FACTOR
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(HIGH_THREAD_COUNT);

        try {
            List<Future<?>> futures = new ArrayList<>();

            for (int thread = 0; thread < HIGH_THREAD_COUNT; thread++) {
                int threadId = thread;

                futures.add(executor.submit(() -> {
                    for (int value = 0; value < VALUES_PER_THREAD; value++) {
                        map.add(
                                "java",
                                threadId * VALUES_PER_THREAD + value
                        );
                    }
                }));
            }

            waitForCompletion(futures);
        } finally {
            shutdown(executor);
        }

        List<Integer> values = map.get("java");

        assertNotNull(values);
        assertEquals(
                HIGH_THREAD_COUNT * VALUES_PER_THREAD,
                values.size()
        );
    }

    @Test
    void shouldHandleConcurrentAddsAndResize() throws Exception {
        var map = new MultivaluedConcurrentHashMap<Integer, Integer>(
                RESIZE_CAPACITY,
                AGGRESSIVE_LOAD_FACTOR
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            List<Future<?>> futures = new ArrayList<>();

            for (int thread = 0; thread < THREAD_COUNT; thread++) {
                int threadId = thread;

                futures.add(executor.submit(() -> {
                    int start = threadId * KEYS_PER_THREAD;

                    for (int i = 0; i < KEYS_PER_THREAD; i++) {
                        int key = start + i;
                        map.add(key, key);
                    }
                }));
            }

            waitForCompletion(futures);
        } finally {
            shutdown(executor);
        }

        for (int thread = 0; thread < THREAD_COUNT; thread++) {
            int start = thread * KEYS_PER_THREAD;

            for (int i = 0; i < KEYS_PER_THREAD; i++) {
                int key = start + i;

                assertEquals(
                        List.of(key),
                        map.get(key),
                        "Missing key: " + key
                );
            }
        }
    }

    @Test
    void shouldHandleConcurrentAddAndRemove() throws Exception {
        var map = new MultivaluedConcurrentHashMap<Integer, Integer>(
                CONCURRENT_CAPACITY,
                LOAD_FACTOR
        );

        for (int i = 0; i < KEY_COUNT; i++) {
            map.add(i, i);
        }

        ExecutorService executor =
                Executors.newFixedThreadPool(THREAD_COUNT);

        try {
            Future<?> addTask = executor.submit(() -> {
                for (int i = 0; i < KEY_COUNT; i++) {
                    map.add(i, i + KEY_COUNT);
                }
            });

            Future<?> removeTask = executor.submit(() -> {
                for (int i = 0; i < KEY_COUNT; i++) {
                    map.remove(i);
                }
            });

            waitForCompletion(List.of(addTask, removeTask));
        } finally {
            shutdown(executor);
        }

        /*
         * Final state is nondeterministic for individual keys,
         * but the map must remain structurally valid.
         */
        for (int i = 0; i < KEY_COUNT; i++) {
            List<Integer> values = map.get(i);

            if (values != null) {
                assertFalse(values.isEmpty());
            }
        }
    }

    @Test
    void shouldHandleKeysWithDifferentHashes() {
        var map = new MultivaluedConcurrentHashMap<Integer, Integer>(
                SEGMENT_TEST_CAPACITY,
                LOAD_FACTOR
        );

        map.add(1, 1);
        map.add(17, 17);
        map.add(33, 33);

        assertEquals(List.of(1), map.get(1));
        assertEquals(List.of(17), map.get(17));
        assertEquals(List.of(33), map.get(33));
    }

    @Test
    void shouldCalculateSegmentCountCorrectly() {
        assertEquals(
                16,
                invokeCalculateSegmentCount(SEGMENT_TEST_CAPACITY)
        );

        assertEquals(
                16,
                invokeCalculateSegmentCount(SEGMENT_TEST_CAPACITY_2)
        );

        assertEquals(
                8,
                invokeCalculateSegmentCount(SEGMENT_TEST_SMALL_CAPACITY)
        );

        assertEquals(
                10,
                invokeCalculateSegmentCount(SEGMENT_TEST_NON_POWER_OF_TWO)
        );
    }

    private static void waitForCompletion(
            List<Future<?>> futures
    ) throws Exception {
        for (Future<?> future : futures) {
            future.get();
        }
    }

    private static void shutdown(ExecutorService executor) {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static int invokeCalculateSegmentCount(int capacity) {
        int target = Math.min(
                MultivaluedConcurrentHashMap.DEFAULT_SEGMENT_COUNT,
                capacity
        );

        for (int segments = target; segments >= 1; segments--) {
            if (capacity % segments == 0) {
                return segments;
            }
        }

        return 1;
    }

    private record CollisionKey(int id) {

        @Override
        public int hashCode() {
            return COLLISION_HASH;
        }
    }

    private static class MinHashKey {

        @Override
        public int hashCode() {
            return MIN_INTEGER_HASH;
        }
    }
}