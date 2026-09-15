package com.example.wildwatch.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.wildwatch.service.PhotoIdentificationService;

@RestController
@RequestMapping("/api/photo")
public class PhotoController {

    private static final Logger log = LoggerFactory.getLogger(PhotoController.class);

    private final PhotoIdentificationService photoIdentificationService;

    public PhotoController(PhotoIdentificationService photoIdentificationService) {
        this.photoIdentificationService = photoIdentificationService;
    }

    @PostMapping("/identify")
    public ResponseEntity<Map<String, String>> identify(@RequestParam("image") MultipartFile image,
            @RequestParam(value = "region", required = false) String region) {
        try {
            String combined = photoIdentificationService.identifySpecies(image, region);
            String identification = combined == null ? "" : combined;
            String regionNote = "";
            // If the service appended a short region note separated by a blank line,
            // split on the last double-newline so the full model response remains
            // in `identification` and only the trailing note goes into `regionNote`.
            if (identification.contains("\n\n")) {
                int idx = identification.lastIndexOf("\n\n");
                if (idx >= 0) {
                    regionNote = identification.substring(idx + 2).trim();
                    identification = identification.substring(0, idx).trim();
                }
            }
            return ResponseEntity.ok(Map.of("identification", identification, "regionNote", regionNote));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to identify species from photo", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "I couldn’t identify the species from that image right now."));
        }
    }
}
