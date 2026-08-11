package com.example.invertedindex.tools.executor;

import java.util.ArrayList;
import java.util.List;

class Queue {

    private final Work[] buffer;
    private int head = 0;
    private int tail = 0;
    private int count = 0;
    private List<Long> fullDurations = new ArrayList<>();
    private List<Long> emptyDurations = new ArrayList<>();
    private volatile long fullStart;
    private volatile long emptyStart;

    public Queue(int size){
        if (size<1){
            throw new IllegalArgumentException();
        }
        buffer = new Work[size];
        emptyStart = System.nanoTime();
    }

    public Work pull(){
        if(count == 0){
            throw new IllegalStateException("Queue is empty");
        }

        if(isFull()) {
            fullDurations.add(System.nanoTime() - fullStart);
        }

        Work work = buffer[head];
        buffer[head] = null;
        head = (head+1)%buffer.length;
        count--;
        if(isEmpty()) emptyStart = System.nanoTime();
        return work;
    }

    public boolean push(Work task){
        if(count== buffer.length){
            throw new IllegalStateException("Queue is full");
        }

        if(isEmpty()) {
            emptyDurations.add(System.nanoTime() - emptyStart);
        }

        buffer[tail] = task;
        tail = (tail+1)% buffer.length;
        count++;
        if(isFull()) fullStart = System.nanoTime();
        return true;
    }

    public boolean isEmpty(){
        return count==0;
    }

    public boolean isFull(){
        return count == buffer.length;
    }

    public List<Long> getFullDurations(){
        return fullDurations;
    }

    public List<Long> getEmptyDurations(){
        return emptyDurations;
    }
}
