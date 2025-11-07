package com.example.booktranslator.controller;

import com.example.booktranslator.service.AudioGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AudioControllerTest {

    private AudioGenerationService audioGenerationService;
    private AudioController controller;

    @BeforeEach
    void setUp() {
        audioGenerationService = Mockito.mock(AudioGenerationService.class);
        controller = new AudioController(audioGenerationService);
    }

    @Test
    void streamAudioReturnsNotFoundWhenMissing() {
        when(audioGenerationService.loadAudio("missing.mp3")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.streamAudio("missing.mp3");

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void downloadAudioReturnsAttachment() {
        ByteArrayResource resource = new ByteArrayResource("data".getBytes());
        when(audioGenerationService.loadAudio("file.mp3")).thenReturn(Optional.of(resource));

        ResponseEntity<?> response = controller.downloadAudio("file.mp3");

        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("attachment; filename=file.mp3");
        assertThat(response.getBody()).isEqualTo(resource);
    }
}
