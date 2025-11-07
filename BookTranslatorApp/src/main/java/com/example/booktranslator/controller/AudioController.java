package com.example.booktranslator.controller;

import com.example.booktranslator.service.AudioGenerationService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
public class AudioController {

    private final AudioGenerationService audioGenerationService;

    public AudioController(AudioGenerationService audioGenerationService) {
        this.audioGenerationService = audioGenerationService;
    }

    @GetMapping("/audio/{fileName}")
    public ResponseEntity<Resource> streamAudio(@PathVariable String fileName) {
        Optional<Resource> resource = audioGenerationService.loadAudio(fileName);
        return resource.map(res -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName)
                        .contentType(MediaType.valueOf("audio/mpeg"))
                        .body(res))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/audio/{fileName}/download")
    public ResponseEntity<Resource> downloadAudio(@PathVariable String fileName) {
        Optional<Resource> resource = audioGenerationService.loadAudio(fileName);
        return resource.map(res -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(res))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
