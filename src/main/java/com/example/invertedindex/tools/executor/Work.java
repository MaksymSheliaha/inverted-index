package com.example.invertedindex.tools.executor;

import com.example.invertedindex.tools.executor.custom.CustomFuture;

import java.util.concurrent.Callable;

public record Work(Callable task, CustomFuture future) implements Runnable {
    @Override
    public void run() {
        future.markStarted();
        try {
            var result = task.call();
            future.set(result);

        } catch (Exception e) {
            future.error();
        }
    }
}
