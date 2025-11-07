package com.example.booktranslator.service;

import com.example.booktranslator.config.OpenAiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final WebClient webClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public OpenAiClient(WebClient.Builder webClientBuilder,
                        OpenAiProperties properties,
                        ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl(properties.apiUrl())
                .build();
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.apiKey = resolveApiKey();
    }

    public Optional<String> translate(String text, String targetLanguage) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> request = Map.of(
                    "model", properties.chatModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", "You translate English literary text to %s. Maintain sentence boundaries and do not add commentary.".formatted(targetLanguage)),
                            Map.of("role", "user", "content", text)
                    ),
                    "temperature", 0.2
            );

            String response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (response == null) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(choices.get(0).path("message").path("content").asText(null));
        } catch (Exception ex) {
            log.error("OpenAI translation request failed", ex);
            return Optional.empty();
        }
    }

    public Optional<byte[]> synthesizeSpeech(String text) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> request = Map.of(
                    "model", properties.audioModel(),
                    "voice", "alloy",
                    "input", text,
                    "format", "mp3"
            );
            return Optional.ofNullable(webClient.post()
                    .uri("/audio/speech")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block());
        } catch (Exception ex) {
            log.error("OpenAI text-to-speech request failed", ex);
            return Optional.empty();
        }
    }

    private boolean isConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API key not configured. Set OPENAI_API_KEY environment variable.");
            return false;
        }
        return true;
    }

    private String resolveApiKey() {
        return Optional.ofNullable(System.getenv("OPENAI_API_KEY"))
                .orElseGet(() -> System.getProperty("openai.api.key"));
    }
}
