package com.example.booktranslator.model;

import java.util.List;

public record TranslationResult(
        BookSummary book,
        List<TranslationSegment> segments,
        String audioFileName
) {
}
