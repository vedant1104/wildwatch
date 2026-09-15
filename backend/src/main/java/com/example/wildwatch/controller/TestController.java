package com.example.wildwatch.controller;

import com.example.wildwatch.client.GbifClient;
import com.example.wildwatch.client.INaturalistClient;
import com.example.wildwatch.client.dto.GbifOccurrenceResponse;
import com.example.wildwatch.client.dto.INaturalistObservationResponse;
import com.example.wildwatch.service.IngestionService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/api/test")
public class TestController {

    private final GbifClient gbifClient;
    private final INaturalistClient iNaturalistClient;
    private final IngestionService ingestionService;

    public TestController(GbifClient gbifClient, INaturalistClient iNaturalistClient,
            IngestionService ingestionService) {
        this.gbifClient = gbifClient;
        this.iNaturalistClient = iNaturalistClient;
        this.ingestionService = ingestionService;
    }

    @GetMapping("/gbif")
    public GbifOccurrenceResponse testGbif(@RequestParam String state) {
        return gbifClient.searchOccurrences("IN", state, 5, 0);
    }

    @GetMapping("/inaturalist")
    public INaturalistObservationResponse testInaturalist(@RequestParam Integer placeId) {
        return iNaturalistClient.getObservations(placeId, 5, 1);
    }

    @PostMapping("/ingest/gbif")
    public int ingestGbif(@RequestParam String country, @RequestParam String stateProvince) {
        return ingestionService.ingestGbifOccurrences(country, stateProvince);
    }

    @PostMapping("/ingest/gbif-history")
    public ResponseEntity<Map<String, Integer>> ingestGbifHistory(@RequestParam String country, @RequestParam String stateProvince) {
        int ingested = ingestionService.ingestGbifOccurrencesAcrossYears(country, stateProvince);
        return ResponseEntity.ok(Map.of("ingested", ingested));
    }

    @PostMapping("/ingest/inaturalist")
    public int ingestInaturalist(@RequestParam Integer placeId, @RequestParam(required = false) String region) {
        return ingestionService.ingestINaturalistObservations(placeId, region);
    }

    @PostMapping("/backfill-region")
    public ResponseEntity<Map<String, Integer>> backfillRegion(@RequestParam String source,
            @RequestParam String region) {
        int updated = ingestionService.backfillRegion(source, region);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @PostMapping("/backfill-region-smart")
    public ResponseEntity<Map<String, Integer>> backfillRegionSmart() {
        int updated = ingestionService.backfillRegionSmart();
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
