package com.example.booktranslator.service;

import com.example.booktranslator.config.BookSourceProperties;
import com.example.booktranslator.model.BookSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PublicDomainBookService {

    private static final Logger log = LoggerFactory.getLogger(PublicDomainBookService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final BookSourceProperties properties;

    public PublicDomainBookService(WebClient.Builder webClientBuilder,
                                   ObjectMapper objectMapper,
                                   BookSourceProperties properties) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public List<BookSummary> searchBooks(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.searchUrl())
                .queryParam("search", query)
                .build(true)
                .toUri();
        try {
            String response = webClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(throwable -> {
                        log.warn("Failed to fetch search results", throwable);
                        return Mono.empty();
                    })
                    .block();
            if (response == null) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results");
            List<BookSummary> summaries = new ArrayList<>();
            for (JsonNode node : results) {
                long id = node.path("id").asLong();
                String title = node.path("title").asText();
                JsonNode authors = node.path("authors");
                String author = authors.isArray() && !authors.isEmpty()
                        ? authors.get(0).path("name").asText()
                        : "Unknown";
                JsonNode formats = node.path("formats");
                String downloadUrl = firstAvailableTextUrl(formats);
                summaries.add(new BookSummary(id, title, author, downloadUrl));
            }
            return summaries;
        } catch (Exception ex) {
            log.error("Error parsing search response", ex);
            return List.of();
        }
    }

    public Optional<String> downloadBookText(long id, String downloadUrl) {
        String url = downloadUrl;
        if (url == null || url.isBlank()) {
            url = UriComponentsBuilder.fromHttpUrl(properties.searchUrl())
                    .pathSegment(String.valueOf(id))
                    .build(true)
                    .toUriString();
        }
        try {
            String payload = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (payload == null) {
                return Optional.empty();
            }
            return Optional.of(cleanPayload(payload));
        } catch (Exception ex) {
            log.error("Failed to download book {}", id, ex);
            return Optional.empty();
        }
    }

    private String cleanPayload(String payload) {
        if (payload.contains("<body")) {
            return payload.replaceAll("<[^>]+>", " ")
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("&amp;", "&")
                    .replaceAll("\s+", " ")
                    .trim();
        }
        return payload;
    }

    private String firstAvailableTextUrl(JsonNode formats) {
        if (formats == null) {
            return null;
        }
        String[] priorities = {
                "text/plain; charset=utf-8",
                "text/plain",
                "text/html",
                "application/octet-stream"
        };
        for (String format : priorities) {
            JsonNode node = formats.get(format);
            if (node != null && node.isTextual()) {
                return node.asText();
            }
        }
        return null;
    }
}
