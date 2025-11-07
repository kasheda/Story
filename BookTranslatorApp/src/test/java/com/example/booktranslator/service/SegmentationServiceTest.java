package com.example.booktranslator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SegmentationServiceTest {

    private SegmentationService segmentationService;

    @BeforeEach
    void setUp() {
        segmentationService = new SegmentationService();
    }

    @Test
    void splitsIntoSentencesRespectingLimit() {
        String text = "This is the first sentence. Here is the second! And the third?";

        List<String> segments = segmentationService.segmentText(text, "sentences", 2);

        assertThat(segments)
                .hasSize(2)
                .containsExactly(
                        "This is the first sentence.",
                        "Here is the second!"
                );
    }

    @Test
    void splitsIntoPagesByCharacterCount() {
        String text = "a".repeat(2500);

        List<String> segments = segmentationService.segmentText(text, "pages", 3);

        assertThat(segments)
                .hasSize(3)
                .allSatisfy(segment -> assertThat(segment.length()).isLessThanOrEqualTo(1200));
    }

    @Test
    void splitsIntoChaptersWithFallback() {
        String text = "Chapter One\nContent.\nchapter two\nMore content.";

        List<String> segments = segmentationService.segmentText(text, "chapters", 5);

        assertThat(segments)
                .hasSize(2)
                .containsExactly(
                        "One\nContent.",
                        "two\nMore content."
                );

        List<String> fallback = segmentationService.segmentText("No chapters here.", "chapters", 1);
        assertThat(fallback)
                .hasSize(1)
                .containsExactly("No chapters here.");
    }
}
