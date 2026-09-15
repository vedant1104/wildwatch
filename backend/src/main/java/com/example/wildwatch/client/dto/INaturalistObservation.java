package com.example.wildwatch.client.dto;

public record INaturalistObservation(
        Long id,
        String taxonName,
        String preferredCommonName,
        String observedOn,
        String location,
        String qualityGrade,
        String photoUrl,
        String placeGuess) {
}
