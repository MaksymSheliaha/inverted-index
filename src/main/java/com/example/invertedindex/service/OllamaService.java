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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            chatClient.prompt().user("Check").call().content();
            log.info("Ollama is available");
        } catch (Exception e) {
            log.error("Ollama health check failed", e);
            throw new IllegalStateException("Ollama is not available", e);
        }
    }

    public boolean isAvailable() {
        try {
            chatClient.prompt().user("Check").call().content();
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

            String response = chatClient.prompt()
                    .user(originalQuery)
                    .call()
                    .content();
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
