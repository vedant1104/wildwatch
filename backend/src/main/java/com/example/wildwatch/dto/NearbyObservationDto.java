package com.example.wildwatch.dto;

import java.time.LocalDate;

public record NearbyObservationDto(
        String speciesName,
        LocalDate date,
        Double lat,
        Double lng,
        String photoUrl
) {
}
