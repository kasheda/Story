package com.example.booktranslator.controller;

import com.example.booktranslator.model.BookSummary;
import com.example.booktranslator.model.TranslationRequest;
import com.example.booktranslator.model.TranslationResult;
import com.example.booktranslator.model.TranslationSegment;
import com.example.booktranslator.service.AudioGenerationService;
import com.example.booktranslator.service.OpenAiTranslationService;
import com.example.booktranslator.service.PublicDomainBookService;
import com.example.booktranslator.service.SegmentationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class TranslationControllerTest {

    private PublicDomainBookService bookService;
    private SegmentationService segmentationService;
    private OpenAiTranslationService translationService;
    private AudioGenerationService audioGenerationService;
    private TranslationController controller;

    @BeforeEach
    void setUp() {
        bookService = Mockito.mock(PublicDomainBookService.class);
        segmentationService = Mockito.mock(SegmentationService.class);
        translationService = Mockito.mock(OpenAiTranslationService.class);
        audioGenerationService = Mockito.mock(AudioGenerationService.class);
        controller = new TranslationController(bookService, segmentationService, translationService, audioGenerationService);
    }

    @Test
    void searchReturnsIndexWhenValidationFails() {
        TranslationRequest request = new TranslationRequest();
        BindingResult bindingResult = new BeanPropertyBindingResult(request, "translationRequest");
        bindingResult.reject("error");
        Model model = new ExtendedModelMap();

        String viewName = controller.searchBooks(request, bindingResult, model);

        assertThat(viewName).isEqualTo("index");
    }

    @Test
    void translateReturnsIndexWhenBookDownloadFails() {
        Model model = new ExtendedModelMap();
        when(bookService.downloadBookText(1L, "")).thenReturn(Optional.empty());

        String viewName = controller.translate(1L, "", "Title", 5, "sentences", model);

        assertThat(viewName).isEqualTo("index");
        assertThat(model.getAttribute("error")).isEqualTo("Unable to download the selected book. Please try another title.");
    }

    @Test
    void translatePopulatesModelOnSuccess() {
        Model model = new ExtendedModelMap();
        when(bookService.downloadBookText(1L, "url")).thenReturn(Optional.of("Hello world."));
        when(segmentationService.segmentText("Hello world.", "sentences", 3)).thenReturn(List.of("Hello world."));
        List<TranslationSegment> segments = List.of(new TranslationSegment(1, "Hello world.", "Hola mundo."));
        when(translationService.translateSegments(List.of("Hello world."), "Spanish")).thenReturn(segments);
        when(audioGenerationService.generateAudio(segments)).thenReturn(Optional.of("file.mp3"));

        String viewName = controller.translate(1L, "url", "Title", 3, "sentences", model);

        assertThat(viewName).isEqualTo("translation");
        TranslationResult result = (TranslationResult) model.getAttribute("translationResult");
        assertThat(result.book()).isEqualTo(new BookSummary(1L, "Title", "", "url"));
        assertThat(result.audioFileName()).isEqualTo("file.mp3");
        assertThat(result.segments()).isEqualTo(segments);
        assertThat(model.getAttribute("segmentCount")).isEqualTo(1);
    }
}
