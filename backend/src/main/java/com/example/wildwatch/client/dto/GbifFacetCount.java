package com.example.wildwatch.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GbifFacetCount(String name, long count) {
}
