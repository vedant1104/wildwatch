package com.example.wildwatch.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.wildwatch.search.SpeciesDocument;
import com.example.wildwatch.search.SpeciesSearchRepository;
import org.springframework.stereotype.Service;

import com.example.wildwatch.dto.SpeciesSummaryDto;
import com.example.wildwatch.dto.YearlyTrendDto;
import com.example.wildwatch.entity.Species;
import com.example.wildwatch.repository.ObservationRepository;
import com.example.wildwatch.repository.SpeciesRepository;

@Service
public class SpeciesQueryService {

    private final SpeciesRepository speciesRepository;
    private final ObservationRepository observationRepository;
    private final SpeciesSearchRepository speciesSearchRepository;

    public SpeciesQueryService(SpeciesRepository speciesRepository,
            ObservationRepository observationRepository,
            SpeciesSearchRepository speciesSearchRepository) {
        this.speciesRepository = speciesRepository;
        this.observationRepository = observationRepository;
        this.speciesSearchRepository = speciesSearchRepository;
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

    public List<SpeciesSummaryDto> searchSpecies(String query, String region) {
        if (query == null || query.isBlank()) {
            return getSpeciesByRegion(region);
        }

        String normalized = query.trim();
        List<SpeciesDocument> docs = speciesSearchRepository.searchByQuery(normalized);
        if (docs.isEmpty()) {
            return List.of();
        }

        Set<Long> matchedIds = docs.stream()
                .map(SpeciesDocument::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (matchedIds.isEmpty()) {
            return List.of();
        }

        List<Species> matchedSpecies = speciesRepository.findAllById(matchedIds);
        List<SpeciesSummaryDto> results = new ArrayList<>();
        for (Species species : matchedSpecies) {
            long count = observationRepository.countBySpeciesIdAndRegion(species.getId(), region);
            results.add(new SpeciesSummaryDto(species.getScientificName(), species.getCommonName(), count));
        }

        results.sort((a, b) -> Long.compare(b.observationCount(), a.observationCount()));
        return results;
    }
}
