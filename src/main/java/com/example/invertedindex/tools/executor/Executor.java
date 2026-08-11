package com.example.invertedindex.tools.executor;

import com.example.invertedindex.tools.executor.custom.CustomFuture;

import java.io.Closeable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.Callable;

public class Executor implements Closeable {

    private static final int DEFAULT_THREAD_NUM = 6;
    private static final int DEFAULT_QUEUE_SIZE = 20;

    private final Thread[] threads;
    // todo: implement thread safe queue
    private final Queue<Runnable> queue;

    private final Object stoppedMonitor = new Object();
    private volatile boolean stopped = false;
    private volatile boolean started = false;
    private volatile boolean closed = false;
    private volatile boolean interrupted = false;


    public Executor() {
        this(DEFAULT_THREAD_NUM, DEFAULT_QUEUE_SIZE);
    }

    public Executor(int threadNum) {
        this(threadNum, DEFAULT_QUEUE_SIZE);
    }

    public Executor(int threadNum, int queueSize) {
        threads = new Thread[threadNum];
        queue = new ArrayDeque<>(queueSize);
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new Worker();
        }
    }

    public synchronized void start(){
        if(started || closed) throw new IllegalStateException();
        started = true;
        Arrays.stream(threads).forEach(Thread::start);
    }

    public synchronized void stop(){
        if(closed) throw new IllegalStateException();
        stopped = true;
        synchronized (queue){
            queue.notifyAll();
        }
    }

    public synchronized void resume(){
        if(closed) throw new IllegalStateException();
        if(stopped){
            synchronized (stoppedMonitor){
                stopped = false;
                stoppedMonitor.notifyAll();
            }
        }
    }

    public synchronized void closeUnsafe(){
        interrupted=true;
        close();
    }

    @Override
    public synchronized void close(){
        if(closed) throw new IllegalStateException();
        closed = true;
        stopped = false;

        if(!started) return;

        synchronized (queue){
            queue.notifyAll();
        }
        synchronized (stoppedMonitor){
            stoppedMonitor.notifyAll();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public <T> CustomFuture<T> execute(Callable<T> task){
        CustomFuture<T> result = new CustomFuture<T>();
        Runnable runnable =  () -> {
            try {
                var res = task.call();
                result.set(res);
            } catch (Exception e) {
                result.error();
            }
        };
        submit(runnable);
        return result;
    }

    public void submit(Runnable task){
        synchronized (queue){
            if(closed || !started) throw new IllegalStateException();

            queue.add(task);
            queue.notify();
        }
    }

    private final class Worker extends Thread{

        @Override
        public void run() {
            while(true) {
                if(interrupted) break;
                if(stopped){
                    synchronized (stoppedMonitor){
                        try {
                            stoppedMonitor.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    continue;
                }

                Runnable task;
                synchronized (queue){
                    while ((queue.isEmpty()&&!closed) && !interrupted){
                        try{
                            queue.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if(stopped) continue;
                    if(interrupted || (closed && queue.isEmpty())) break;
                    task = queue.poll();
                }

                task.run();
            }
            System.out.printf("Thread %d closed\n", Thread.currentThread().threadId());
        }
    }
}
