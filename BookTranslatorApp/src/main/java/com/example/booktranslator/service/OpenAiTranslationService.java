package com.example.booktranslator.service;

import com.example.booktranslator.config.OpenAiProperties;
import com.example.booktranslator.model.TranslationSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAiTranslationService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiTranslationService.class);

    private final OpenAiClient openAiClient;
    private final OpenAiProperties properties;

    public OpenAiTranslationService(OpenAiClient openAiClient, OpenAiProperties properties) {
        this.openAiClient = openAiClient;
        this.properties = properties;
    }

    public List<TranslationSegment> translateSegments(List<String> segments, String targetLanguage) {
        int limit = properties.maxSentences() != null
                ? Math.min(properties.maxSentences(), segments.size())
                : segments.size();
        List<TranslationSegment> results = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String segment = segments.get(i);
            String translation = openAiClient.translate(segment, targetLanguage)
                    .orElseGet(() -> {
                        log.warn("Falling back to source text for segment {}", i);
                        return segment;
                    });
            results.add(new TranslationSegment(i + 1, segment, translation));
        }
        return results;
    }
}
