package com.example.invertedindex.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
public class OllamaConfig {

    @Value("${ollama.defaultPrompt:ollama/prompt.txt}")
    private ClassPathResource defaultPrompt;

    @Value("${ollama.connect-timeout:5s}")
    private Duration connectTimeout;

    @Value("${ollama.read-timeout:15s}")
    private Duration readTimeout;

    @Bean
    public RestClientCustomizer ollamaRestClientCustomizer() {
        return restClientBuilder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(connectTimeout);
            requestFactory.setReadTimeout(readTimeout);
            restClientBuilder.requestFactory(requestFactory);
        };
    }

    @Bean
    public ChatClient getClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(defaultPrompt)
                .build();
    }
}
