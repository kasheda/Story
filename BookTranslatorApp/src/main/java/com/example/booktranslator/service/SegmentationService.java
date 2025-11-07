package com.example.booktranslator.service;

import org.springframework.stereotype.Service;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SegmentationService {

    public List<String> segmentText(String text, String mode, int maxSegments) {
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "pages" -> splitByPages(text, maxSegments);
            case "chapters" -> splitByChapters(text, maxSegments);
            default -> splitBySentences(text, maxSegments);
        };
    }

    private List<String> splitBySentences(String text, int maxSegments) {
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
        iterator.setText(text);
        List<String> sentences = new ArrayList<>();
        int start = iterator.first();
        int end = iterator.next();
        while (end != BreakIterator.DONE && sentences.size() < maxSegments) {
            String sentence = text.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            start = end;
            end = iterator.next();
        }
        return sentences;
    }

    private List<String> splitByPages(String text, int maxSegments) {
        int charsPerPage = 1200;
        return splitBySize(text, charsPerPage, maxSegments);
    }

    private List<String> splitByChapters(String text, int maxSegments) {
        String[] chapters = text.split("(?i)\\bchapter\\b");
        List<String> results = new ArrayList<>();
        for (String chapter : chapters) {
            if (chapter.isBlank()) {
                continue;
            }
            results.add(chapter.trim());
            if (results.size() >= maxSegments) {
                break;
            }
        }
        if (results.isEmpty()) {
            results.addAll(splitBySentences(text, maxSegments));
        }
        return results;
    }

    private List<String> splitBySize(String text, int size, int maxSegments) {
        List<String> segments = new ArrayList<>();
        int start = 0;
        while (start < text.length() && segments.size() < maxSegments) {
            int end = Math.min(start + size, text.length());
            segments.add(text.substring(start, end).trim());
            start = end;
        }
        return segments;
    }
}
