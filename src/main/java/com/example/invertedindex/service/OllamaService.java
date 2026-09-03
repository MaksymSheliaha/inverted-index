package com.example.invertedindex.service;

import com.example.invertedindex.index.AnalyzeUtils;
import com.example.invertedindex.model.Token;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {
    private static final long TIMEOUT_SECONDS = 10;
    private static final long INIT_TIMEOUT_SECONDS = 120;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            executeWithTimeout(() -> chatClient.prompt().user("Check").call().content(), INIT_TIMEOUT_SECONDS);
            log.info("Ollama is available");
        } catch (Exception e) {
            log.error("Ollama health check failed", e);
            throw new IllegalStateException("Ollama is not available", e);
        }
    }

    public boolean isAvailable() {
        try {
            executeWithTimeout(() -> chatClient.prompt().user("Check").call().content(), TIMEOUT_SECONDS);
            return true;
        } catch (Exception e) {
            log.error("Ollama health check failed", e);
            return false;
        }
    }

    public List<Token> getTokens(String originalQuery) {
        if (originalQuery == null || originalQuery.isBlank()) {
            return Collections.emptyList();
        }

        try {
            log.info("Expanding query: '{}'", originalQuery);

            String response = executeWithTimeout(() -> chatClient.prompt()
                            .user(originalQuery)
                            .call()
                            .content(),
                    TIMEOUT_SECONDS);
            log.debug("LLM Response: {}", response);

            List<Token> keywords = parseResponse(response);
            log.info("Generated keywords: {}", keywords);

            return analyzeTokens(keywords);
        } catch (Exception e) {
            log.error("Ollama failed. Falling back to original query keywords.", e);
            return Arrays.stream(StringUtils.tokenizeToStringArray(originalQuery, " \t\n\r\f"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(str -> new Token(str, Collections.emptyList()))
                    .collect(Collectors.toList());
        }
    }

    private <T> T executeWithTimeout(Supplier<T> supplier, long timeout) throws Exception {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier);
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("Ollama request timed out after " + TIMEOUT_SECONDS + " seconds", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new IllegalStateException("Unexpected Ollama request failure", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private List<Token> parseResponse(String response) throws JsonProcessingException {
        if (response != null) {
            response = response.trim();
            if (response.startsWith("```json")) {
                response = response.substring(7);
            } else if (response.startsWith("```")) {
                response = response.substring(3);
            }
            if (response.endsWith("```")) {
                response = response.substring(0, response.length() - 3);
            }
            response = response.trim();
        }

        return objectMapper.readValue(response, new TypeReference<>() {});
    }

    private List<Token> analyzeTokens(List<Token> tokens) {
        return tokens.stream()
                .map(this::analyzeToken).toList();
    }

    private Token analyzeToken(Token token) {
        String origToken = AnalyzeUtils.analyze(token.origToken(), false).getFirst();
        List<String> synonyms = token.synonyms().stream().map(AnalyzeUtils::analyze).flatMap(List::stream).toList();
        return new Token(origToken, synonyms);
    }
}
