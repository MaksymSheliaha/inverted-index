package com.example.invertedindex.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {
    private final ChatClient chatClient;

    public List<String> expandQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.isBlank()) {
            return Collections.emptyList();
        }

        try {
            log.info("Expanding query: '{}'", originalQuery);

            String response = chatClient.prompt()
                    .user(originalQuery)
                    .call()
                    .content();

            List<String> keywords = Arrays.stream(response.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            log.info("Generated keywords: {}", keywords);
            return keywords;

        } catch (Exception e) {
            log.error("Ollama failed. Falling back to original query keywords.", e);
            return Arrays.stream(originalQuery.split("\\s+"))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
    }

}
