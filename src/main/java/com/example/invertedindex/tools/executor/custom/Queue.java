package com.example.invertedindex.tools.executor.custom;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class Queue<T> {
    private AtomicInteger count;
    private AtomicReference<Node<T>> head;
    private AtomicReference<Node<T>> tail;

//    public Queue(int size){
//        if (size<1){
//            throw new IllegalArgumentException();
//        }
//        buffer = (T[]) new Object[size];
//    }
//
//    public T pull(){
//        if(count == 0){
//            throw new IllegalStateException("Queue is empty");
//        }
//        var work = buffer[head];
//        buffer[head] = null;
//        head = (head+1)%buffer.length;
//        count--;
//        return work;
//    }
//
//    public boolean push(T task){
//        if(count== buffer.length){
//            throw new IllegalStateException("Queue is full");
//        }
//
//        buffer[tail] = task;
//        tail = (tail+1)% buffer.length;
//        count++;
//        return true;
//    }
//
//    public boolean isEmpty(){
//        return head.get() == null;
//    }

    private record Node<T>(T value, Node<T> next, Node<T> prev){}
}
