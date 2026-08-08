package com.example.invertedindex.model;

import java.util.concurrent.atomic.AtomicInteger;

public class CyclicCounter {
    private final int maxVal;
    private final AtomicInteger ai = new AtomicInteger(0);

    public CyclicCounter(int maxVal) {
        this.maxVal = maxVal;
    }

    public int increment() {
        int curVal, newVal;
        do {
            curVal = this.ai.get();
            newVal = (curVal + 1) % this.maxVal;
        } while (!this.ai.compareAndSet(curVal, newVal));
        return newVal;
    }
}
