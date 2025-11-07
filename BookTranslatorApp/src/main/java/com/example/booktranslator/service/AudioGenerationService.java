package com.example.booktranslator.service;

import com.example.booktranslator.model.TranslationSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AudioGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AudioGenerationService.class);

    private final OpenAiClient openAiClient;
    private final Path outputDirectory;

    public AudioGenerationService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
        this.outputDirectory = Paths.get("audio-output");
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException ex) {
            log.warn("Unable to create audio output directory {}", outputDirectory, ex);
        }
    }

    public Optional<String> generateAudio(List<TranslationSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return Optional.empty();
        }
        String script = segments.stream()
                .map(segment -> segment.source() + System.lineSeparator() + segment.translated())
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
        return openAiClient.synthesizeSpeech(script)
                .flatMap(bytes -> writeFile(bytes, "translation-" + UUID.randomUUID() + ".mp3"));
    }

    public Optional<Resource> loadAudio(String fileName) {
        Path filePath = outputDirectory.resolve(fileName);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(filePath.toFile()));
    }

    private Optional<String> writeFile(byte[] data, String fileName) {
        Path filePath = outputDirectory.resolve(fileName);
        try {
            Files.write(filePath, data);
            return Optional.of(fileName);
        } catch (IOException ex) {
            log.error("Failed to write audio file {}", filePath, ex);
            return Optional.empty();
        }
    }
}
