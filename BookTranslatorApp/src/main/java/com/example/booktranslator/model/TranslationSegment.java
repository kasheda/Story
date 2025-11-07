package com.example.booktranslator.model;

public record TranslationSegment(
        int index,
        String source,
        String translated
) {
}
