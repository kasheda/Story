package com.example.booktranslator.service;

import com.example.booktranslator.config.OpenAiProperties;
import com.example.booktranslator.model.TranslationSegment;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class OpenAiTranslationServiceTest {

    @Test
    void respectsMaxSentenceLimitAndFallsBackToSource() {
        OpenAiClient openAiClient = Mockito.mock(OpenAiClient.class);
        when(openAiClient.translate("first", "Spanish")).thenReturn(java.util.Optional.of("first -> Spanish"));
        when(openAiClient.translate("second", "Spanish")).thenReturn(java.util.Optional.of("second -> Spanish"));
        when(openAiClient.translate("missing translation", "Spanish")).thenReturn(java.util.Optional.empty());

        OpenAiProperties properties = new OpenAiProperties("", "", "", 2);
        OpenAiTranslationService service = new OpenAiTranslationService(openAiClient, properties);

        List<String> segments = List.of("first", "second", "missing translation");

        List<TranslationSegment> result = service.translateSegments(segments, "Spanish");

        assertThat(result)
                .hasSize(2)
                .extracting(TranslationSegment::translated)
                .containsExactly("first -> Spanish", "second -> Spanish");

        TranslationSegment fallback = service.translateSegments(List.of("missing translation"), "Spanish").get(0);
        assertThat(fallback.translated()).isEqualTo("missing translation");
    }
}
