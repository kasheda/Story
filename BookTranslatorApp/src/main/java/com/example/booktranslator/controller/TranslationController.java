package com.example.booktranslator.controller;

import com.example.booktranslator.model.BookSummary;
import com.example.booktranslator.model.TranslationRequest;
import com.example.booktranslator.model.TranslationResult;
import com.example.booktranslator.model.TranslationSegment;
import com.example.booktranslator.service.AudioGenerationService;
import com.example.booktranslator.service.OpenAiTranslationService;
import com.example.booktranslator.service.PublicDomainBookService;
import com.example.booktranslator.service.SegmentationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class TranslationController {

    private final PublicDomainBookService bookService;
    private final SegmentationService segmentationService;
    private final OpenAiTranslationService translationService;
    private final AudioGenerationService audioGenerationService;

    public TranslationController(PublicDomainBookService bookService,
                                 SegmentationService segmentationService,
                                 OpenAiTranslationService translationService,
                                 AudioGenerationService audioGenerationService) {
        this.bookService = bookService;
        this.segmentationService = segmentationService;
        this.translationService = translationService;
        this.audioGenerationService = audioGenerationService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("translationRequest", new TranslationRequest());
        return "index";
    }

    @PostMapping("/search")
    public String searchBooks(@Valid @ModelAttribute("translationRequest") TranslationRequest request,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "index";
        }
        List<BookSummary> results = bookService.searchBooks(request.getTitle());
        model.addAttribute("results", results);
        model.addAttribute("segments", request.getSegments());
        model.addAttribute("segmentationMode", request.getSegmentationMode());
        model.addAttribute("searchedTitle", request.getTitle());
        if (results.isEmpty()) {
            model.addAttribute("noResults", true);
        }
        return "search-results";
    }

    @PostMapping("/translate")
    public String translate(@RequestParam("bookId") long bookId,
                            @RequestParam(value = "downloadUrl", required = false) String downloadUrl,
                            @RequestParam("title") String title,
                            @RequestParam("segments") int segments,
                            @RequestParam("segmentationMode") String segmentationMode,
                            Model model) {
        int safeSegmentCount = Math.max(1, Math.min(segments, 200));

        Optional<String> bookText = bookService.downloadBookText(bookId, downloadUrl);
        if (bookText.isEmpty()) {
            model.addAttribute("error", "Unable to download the selected book. Please try another title.");
            model.addAttribute("translationRequest", new TranslationRequest());
            return "index";
        }
        List<String> segmented = segmentationService.segmentText(bookText.get(), segmentationMode, safeSegmentCount);
        if (segmented.isEmpty()) {
            model.addAttribute("error", "No content extracted from book for translation.");
            model.addAttribute("translationRequest", new TranslationRequest());
            return "index";
        }
        List<TranslationSegment> translations = translationService.translateSegments(segmented, "Spanish");
        Optional<String> audioFileName = audioGenerationService.generateAudio(translations);

        TranslationResult result = new TranslationResult(
                new BookSummary(bookId, title, "", downloadUrl),
                translations,
                audioFileName.orElse(null)
        );
        model.addAttribute("translationResult", result);
        model.addAttribute("segmentationMode", segmentationMode);
        model.addAttribute("segmentCount", translations.size());
        return "translation";
    }
}
