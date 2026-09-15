package com.example.wildwatch.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.wildwatch.dto.SpeciesSummaryDto;
import com.example.wildwatch.dto.YearlyTrendDto;
import com.example.wildwatch.repository.ObservationRepository;
import com.example.wildwatch.repository.SpeciesRepository;

@Service
public class SpeciesQueryService {

    private final SpeciesRepository speciesRepository;
    private final ObservationRepository observationRepository;

    public SpeciesQueryService(SpeciesRepository speciesRepository, ObservationRepository observationRepository) {
        this.speciesRepository = speciesRepository;
        this.observationRepository = observationRepository;
    }

    public List<SpeciesSummaryDto> getSpeciesByRegion(String region) {
        // Return species summaries ordered by observation count descending
        return speciesRepository.findSpeciesSummaryByRegion(region);
    }

    public List<YearlyTrendDto> getYearlyTrend(String scientificName, String region) {
        return observationRepository.findYearlyTrendByScientificNameAndRegion(scientificName, region).stream()
                .map(row -> new YearlyTrendDto(
                        row[0] == null ? null : ((Number) row[0]).intValue(),
                        row[1] == null ? 0L : ((Number) row[1]).longValue()))
                .toList();
    }
}
