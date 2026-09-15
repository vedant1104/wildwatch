package com.example.wildwatch.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GbifOccurrenceResponse(
        int count,
        int offset,
        int limit,
        List<GbifOccurrence> results,
        List<GbifFacet> facets) {
}
