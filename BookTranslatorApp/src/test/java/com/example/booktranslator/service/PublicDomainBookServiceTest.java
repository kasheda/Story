package com.example.booktranslator.service;

import com.example.booktranslator.config.BookSourceProperties;
import com.example.booktranslator.model.BookSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PublicDomainBookServiceTest {

    private RecordingExchangeFunction exchangeFunction;
    private PublicDomainBookService service;

    @BeforeEach
    void setUp() {
        exchangeFunction = new RecordingExchangeFunction();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        BookSourceProperties properties = new BookSourceProperties("https://example.org/books", 100);
        service = new PublicDomainBookService(builder, new ObjectMapper(), properties);
    }

    @Test
    void searchBooksParsesResultsAndHandlesEmpty() {
        exchangeFunction.registerResponse(
                "https://example.org/books?search=don+quixote",
                "{\"results\":[{\"id\":1,\"title\":\"Don Quixote\",\"authors\":[{\"name\":\"Miguel de Cervantes\"}],\"formats\":{\"text/plain\":\"https://example.org/book1.txt\"}}]}"
        );

        List<BookSummary> summaries = service.searchBooks("don quixote");

        assertThat(summaries)
                .hasSize(1)
                .first()
                .satisfies(summary -> {
                    assertThat(summary.id()).isEqualTo(1L);
                    assertThat(summary.title()).isEqualTo("Don Quixote");
                    assertThat(summary.author()).isEqualTo("Miguel de Cervantes");
                    assertThat(summary.downloadUrl()).isEqualTo("https://example.org/book1.txt");
                });

        assertThat(service.searchBooks(" ")).isEmpty();
    }

    @Test
    void downloadBookTextCleansHtmlAndHandlesFallbackUrl() {
        exchangeFunction.registerResponse(
                "https://example.org/books/2",
                "<html><body>Content &amp; More</body></html>"
        );

        Optional<String> text = service.downloadBookText(2L, "");

        assertThat(text).contains("Content & More");
    }

    private static class RecordingExchangeFunction implements ExchangeFunction {

        private final Map<String, String> responses = new HashMap<>();

        void registerResponse(String url, String body) {
            responses.put(url, body);
        }

        @Override
        public Mono<ClientResponse> exchange(ClientRequest request) {
            String body = responses.get(request.url().toString());
            if (body == null) {
                return Mono.empty();
            }
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        }
    }
}
