package com.example.invertedindex.service;

import com.example.invertedindex.model.response.HealthResponse;
import com.example.invertedindex.model.response.StatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthService {
    private final OllamaService ollamaService;
    private final IndexService indexService;

    public HealthResponse getHealth() {
        return new HealthResponse("ok", ollamaService.isAvailable());
    }

    public StatsResponse getStats() {
        return new StatsResponse(
                indexService.isIndexed(),
                indexService.getDocumentCount(),
                indexService.getIndexBuildTimeMs(),
                indexService.getThreadsUsed()
        );
    }
}
