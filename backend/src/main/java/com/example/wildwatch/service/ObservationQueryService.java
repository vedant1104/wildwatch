package com.example.wildwatch.service;

import com.example.wildwatch.dto.NearbyObservationDto;
import com.example.wildwatch.repository.ObservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ObservationQueryService {

    private final ObservationRepository observationRepository;

    public ObservationQueryService(ObservationRepository observationRepository) {
        this.observationRepository = observationRepository;
    }

    public List<NearbyObservationDto> findNearby(double lat, double lng, double radiusKm, String region) {
        double radiusMeters = radiusKm * 1000d;
        return observationRepository.findNearbyObservationsByRegion(lat, lng, radiusMeters, region).stream()
                .map(row -> new NearbyObservationDto(
                        row[0] == null ? null : String.valueOf(row[0]),
                        row[1] == null ? null : LocalDate.parse(String.valueOf(row[1])),
                        row[2] == null ? null : ((Number) row[2]).doubleValue(),
                        row[3] == null ? null : ((Number) row[3]).doubleValue(),
                        row[4] == null ? null : String.valueOf(row[4])
                ))
                .toList();
    }
}
