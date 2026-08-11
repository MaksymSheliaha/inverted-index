package com.example.invertedindex.tools.executor;

import com.example.invertedindex.tools.executor.custom.CustomFuture;
import com.example.invertedindex.tools.executor.custom.MyThread;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

public class ThreadPool implements Closeable {

    private static final int DEFAULT_THREAD_NUM = 6;
    private static final int DEFAULT_QUEUE_SIZE = 20;

    private final Thread[] threads;
    private final Queue queue;

    private final Object stoppedMonitor = new Object();
    private volatile boolean stopped = false;
    private volatile boolean started = false;
    private volatile boolean closed = false;
    private volatile boolean interrupted = false;


    public ThreadPool() throws InterruptedException {
        this(DEFAULT_THREAD_NUM, DEFAULT_QUEUE_SIZE);
    }

    public ThreadPool(int threadNum, int queueSize) {
        threads = new Thread[threadNum];
        queue = new Queue(queueSize);
        for (int i = 0; i < threadNum; i++) {
            threads[i] = new MyThread(new Worker());
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
        System.out.println("close called");
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

        System.out.println("waiting threads to finish");
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public <T> CustomFuture<T> execute(Callable<T> task){
        synchronized (queue){
            if(closed || !started) throw  new IllegalStateException();
            if(queue.isFull()){
                System.out.println("pool decline "+task.toString());
                return null;
            } else {
                CustomFuture<T> result = new CustomFuture<T>();
                queue.push(new Work(task, result));
                queue.notify();
                System.out.println("pool except     "+task.toString());
                return result;
            }
        }
    }

    public void submit(Runnable task){
        var result = execute(() -> {
            task.run();
            return null;
        });
        if(result==null) throw new RejectedExecutionException("Pool is full");
    }

    public synchronized List<Long> getQueueFullTimes(){
        if(!closed) throw new IllegalStateException("Pool is not closed");
        return queue.getFullDurations();
    }

    public synchronized List<Long> getQueueEmptyTimes(){
        if(!closed) throw new IllegalStateException("Pool is not closed");
        return queue.getEmptyDurations();
    }

    private final class Worker implements Runnable{

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

                Work work;
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
                    work = queue.pull();
                    System.out.println("pool take " + work.task().toString());
                }

                work.run();
                System.out.println("pool finished "+ work.task());

            }

            System.out.printf("Thread %d closed\n", Thread.currentThread().threadId());
        }
    }
}
