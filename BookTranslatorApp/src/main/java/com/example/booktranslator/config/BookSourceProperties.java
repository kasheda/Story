package com.example.booktranslator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "book.source")
public record BookSourceProperties(
        String searchUrl,
        Integer maxSentences
) {
}
