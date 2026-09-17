package com.example.wildwatch.controller;

import com.example.wildwatch.dto.NearbyObservationDto;
import com.example.wildwatch.dto.SpeciesSummaryDto;
import com.example.wildwatch.dto.YearlyTrendDto;
import com.example.wildwatch.service.ObservationQueryService;
import com.example.wildwatch.service.SpeciesQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SpeciesController {

    private final SpeciesQueryService speciesQueryService;
    private final ObservationQueryService observationQueryService;

    public SpeciesController(SpeciesQueryService speciesQueryService, ObservationQueryService observationQueryService) {
        this.speciesQueryService = speciesQueryService;
        this.observationQueryService = observationQueryService;
    }

    @GetMapping("/species")
    public List<SpeciesSummaryDto> getSpeciesByRegion(@RequestParam String region) {
        return speciesQueryService.getSpeciesByRegion(region);
    }

    @GetMapping("/species/search")
    public List<SpeciesSummaryDto> searchSpecies(@RequestParam String q,
            @RequestParam String region) {
        return speciesQueryService.searchSpecies(q, region);
    }

    @GetMapping("/species/{scientificName}/trend")
    public List<YearlyTrendDto> getYearlyTrend(@PathVariable String scientificName,
            @RequestParam String region) {
        return speciesQueryService.getYearlyTrend(scientificName, region);
    }

    @GetMapping("/observations/nearby")
    public List<NearbyObservationDto> getNearby(@RequestParam double lat,
            @RequestParam double lng,
            @RequestParam double radiusKm,
            @RequestParam(required = false) String region) {
        return observationQueryService.findNearby(lat, lng, radiusKm, region);
    }
}
