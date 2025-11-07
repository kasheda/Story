package com.example.booktranslator.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class TranslationRequest {

    @NotBlank(message = "Please enter a book title")
    private String title;

    @Min(value = 1, message = "Translate at least one segment")
    @Max(value = 200, message = "Limit segments to 200 for performance")
    private int segments = 10;

    @NotBlank(message = "Provide a segmentation mode")
    private String segmentationMode = "sentences";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSegments() {
        return segments;
    }

    public void setSegments(int segments) {
        this.segments = segments;
    }

    public String getSegmentationMode() {
        return segmentationMode;
    }

    public void setSegmentationMode(String segmentationMode) {
        this.segmentationMode = segmentationMode;
    }
}
