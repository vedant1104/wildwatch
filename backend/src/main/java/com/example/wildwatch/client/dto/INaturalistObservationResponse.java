package com.example.wildwatch.client.dto;

import java.util.List;

public record INaturalistObservationResponse(
        int totalResults,
        int page,
        int perPage,
        List<INaturalistObservation> results) {
}
