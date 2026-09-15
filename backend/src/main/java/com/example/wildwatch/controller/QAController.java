package com.example.wildwatch.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.wildwatch.service.WildlifeQAService;

@RestController
@RequestMapping("/api/qa")
public class QAController {

    private static final Logger log = LoggerFactory.getLogger(QAController.class);

    private final WildlifeQAService wildlifeQAService;

    public QAController(WildlifeQAService wildlifeQAService) {
        this.wildlifeQAService = wildlifeQAService;
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(@RequestBody AskRequest request) {
        try {
            String answer = wildlifeQAService.answerQuestion(request.question(), request.region());
            return ResponseEntity.ok(Map.of("answer", answer));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("answer", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to answer wildlife question", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("answer", "I couldn’t answer that from the available wildlife data right now."));
        }
    }

    public record AskRequest(String question, String region) {
    }
}
