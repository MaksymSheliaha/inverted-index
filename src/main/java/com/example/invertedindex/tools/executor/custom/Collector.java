package com.example.invertedindex.tools.executor.custom;

import java.util.ArrayList;
import java.util.List;

public class Collector {
    private final List<CustomFuture<String>> results = new ArrayList<>();

    public synchronized void addResult(CustomFuture<String> result) {
        results.add(result);
    }

    public synchronized List<CustomFuture<String>> getResults(){
        return results;
    }
}
