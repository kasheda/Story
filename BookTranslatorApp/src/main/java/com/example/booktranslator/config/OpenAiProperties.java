package com.example.booktranslator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiUrl,
        String chatModel,
        String audioModel,
        Integer maxSentences
) {
}
