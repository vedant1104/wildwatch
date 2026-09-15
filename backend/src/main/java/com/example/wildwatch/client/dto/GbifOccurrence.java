package com.example.wildwatch.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GbifOccurrence(
        Long key,
        String species,
        String vernacularName,
        String eventDate,
        Double decimalLatitude,
        Double decimalLongitude,
        String basisOfRecord,
        String datasetName,
        Integer year) {
}
