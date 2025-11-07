package com.example.booktranslator.model;

public record BookSummary(
        long id,
        String title,
        String author,
        String downloadUrl
) {
}
