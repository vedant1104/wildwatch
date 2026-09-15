package com.example.wildwatch.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GbifFacet(String field, List<GbifFacetCount> counts) {
}
