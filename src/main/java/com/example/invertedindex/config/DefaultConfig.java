package com.example.invertedindex.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
