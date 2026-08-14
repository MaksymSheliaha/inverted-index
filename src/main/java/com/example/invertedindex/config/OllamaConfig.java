package com.example.invertedindex.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class OllamaConfig {

    @Value("${ollama.defaultPrompt:ollama/prompt.txt}")
    private ClassPathResource defaultPrompt;

    @Bean
    public ChatClient getClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(defaultPrompt)
                .build();
    }
}
