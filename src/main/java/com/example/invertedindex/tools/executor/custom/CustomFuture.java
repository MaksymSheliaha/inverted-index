package com.example.invertedindex.tools.executor.custom;

import java.util.concurrent.atomic.AtomicBoolean;

public class CustomFuture<T> {

    private final AtomicBoolean isReady;
    private T payload;
    private volatile Status status;

    private final long creationTime;
    private long startWorkingTime;

    public CustomFuture(){
        this.isReady = new AtomicBoolean(false);
        this.creationTime = System.nanoTime();
        this.status = Status.CREATED;
    }

    public synchronized void set(T payload){
        isReady.set(true);
        this.payload = payload;
        this.status = Status.DONE;
        notifyAll();
    }

    public synchronized T tryGet(){
        if(isReady()){
            return payload;
        }

        return null;
    }

    public synchronized T get() throws InterruptedException {
        while (!isReady()){
            wait();
        }
        return payload;
    }

    public boolean isReady(){
        return isReady.get();
    }

    public synchronized void error(){
        this.status = Status.ERROR;
    }

    public Status getStatus(){
        return status;
    }

    public void markStarted(){
        startWorkingTime=System.nanoTime();
        this.status = Status.INPROGRESS;
    }

    public long getWaitNanoTime(){
        return startWorkingTime-creationTime;
    }

    public enum Status{
        CREATED, INPROGRESS, DONE, ERROR
    }
}
