package com.example.wildwatch.dto;

public record SpeciesSummaryDto(
        String scientificName,
        String commonName,
        long observationCount) {
}
