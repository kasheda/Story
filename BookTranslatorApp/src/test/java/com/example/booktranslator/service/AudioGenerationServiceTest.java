package com.example.booktranslator.service;

import com.example.booktranslator.model.TranslationSegment;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AudioGenerationServiceTest {

    @Test
    void generatesAudioFileWhenClientReturnsBytes() throws IOException {
        OpenAiClient client = Mockito.mock(OpenAiClient.class);
        when(client.synthesizeSpeech(Mockito.anyString())).thenReturn(Optional.of("audio".getBytes()));
        Path tempDir = Files.createTempDirectory("audio-test");
        AudioGenerationService service = new AudioGenerationService(client, tempDir);

        List<TranslationSegment> segments = List.of(
                new TranslationSegment(1, "Hello", "Hola"),
                new TranslationSegment(2, "World", "Mundo")
        );

        Optional<String> fileName = service.generateAudio(segments);

        assertThat(fileName).isPresent();
        Path audioFile = tempDir.resolve(fileName.get());
        assertThat(Files.exists(audioFile)).isTrue();
        assertThat(Files.readAllBytes(audioFile)).containsExactly("audio".getBytes());
    }

    @Test
    void loadAudioReturnsEmptyForMissingFile() throws IOException {
        OpenAiClient client = Mockito.mock(OpenAiClient.class);
        Path tempDir = Files.createTempDirectory("audio-test-missing");
        AudioGenerationService service = new AudioGenerationService(client, tempDir);

        Optional<Resource> resource = service.loadAudio("missing.mp3");

        assertThat(resource).isEmpty();
    }

    @Test
    void generateAudioReturnsEmptyWhenClientFails() throws IOException {
        OpenAiClient client = Mockito.mock(OpenAiClient.class);
        when(client.synthesizeSpeech(Mockito.anyString())).thenReturn(Optional.empty());
        Path tempDir = Files.createTempDirectory("audio-test-empty");
        AudioGenerationService service = new AudioGenerationService(client, tempDir);

        Optional<String> fileName = service.generateAudio(List.of(new TranslationSegment(1, "Hello", "Hola")));

        assertThat(fileName).isEmpty();
    }
}
