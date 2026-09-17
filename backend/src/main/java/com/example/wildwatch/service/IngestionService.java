package com.example.wildwatch.service;

import com.example.wildwatch.client.GbifClient;
import com.example.wildwatch.client.INaturalistClient;
import com.example.wildwatch.client.dto.GbifOccurrence;
import com.example.wildwatch.client.dto.GbifOccurrenceResponse;
import com.example.wildwatch.client.dto.INaturalistObservation;
import com.example.wildwatch.client.dto.INaturalistObservationResponse;
import com.example.wildwatch.entity.Observation;
import com.example.wildwatch.entity.Species;
import com.example.wildwatch.repository.ObservationRepository;
import com.example.wildwatch.repository.SpeciesRepository;
import com.example.wildwatch.search.SpeciesDocument;
import com.example.wildwatch.search.SpeciesSearchRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final int MAX_RECORDS_PER_RUN = 2000;
    private static final int GBIF_PAGE_SIZE = 50;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final GbifClient gbifClient;
    private final INaturalistClient iNaturalistClient;
    private final SpeciesRepository speciesRepository;
    private final ObservationRepository observationRepository;
    private final SpeciesSearchRepository speciesSearchRepository;

    public IngestionService(GbifClient gbifClient,
            INaturalistClient iNaturalistClient,
            SpeciesRepository speciesRepository,
            ObservationRepository observationRepository,
            SpeciesSearchRepository speciesSearchRepository) {
        this.gbifClient = gbifClient;
        this.iNaturalistClient = iNaturalistClient;
        this.speciesRepository = speciesRepository;
        this.observationRepository = observationRepository;
        this.speciesSearchRepository = speciesSearchRepository;
    }

    @Transactional
    public int ingestGbifOccurrences(String country, String stateProvince) {
        int ingested = 0;
        int skippedDuplicates = 0;
        int offset = 0;

        while (offset < MAX_RECORDS_PER_RUN) {
            GbifOccurrenceResponse response = gbifClient.searchOccurrences(country, stateProvince, GBIF_PAGE_SIZE,
                    offset);
            List<GbifOccurrence> results = response != null && response.results() != null ? response.results()
                    : List.of();

            if (results.isEmpty()) {
                break;
            }

            for (GbifOccurrence occurrence : results) {
                if (occurrence == null || occurrence.species() == null || occurrence.species().isBlank()) {
                    continue;
                }

                Species species = speciesRepository.findByScientificName(occurrence.species())
                        .orElseGet(() -> {
                            Species saved = speciesRepository.save(new Species(
                                    occurrence.species(),
                                    occurrence.vernacularName(),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null));
                            speciesSearchRepository.save(SpeciesDocument.fromEntity(saved, stateProvince));
                            return saved;
                        });
                if (species.getId() != null) {
                    speciesSearchRepository.save(SpeciesDocument.fromEntity(species, stateProvince));
                }

                String externalId = occurrence.key() != null ? String.valueOf(occurrence.key()) : null;
                if (externalId == null
                        || observationRepository.findBySourceAndExternalId("GBIF", externalId).isPresent()) {
                    skippedDuplicates++;
                    continue;
                }

                Observation observation = new Observation(
                        species,
                        "GBIF",
                        parseDate(occurrence.eventDate()),
                        buildPoint(occurrence.decimalLatitude(), occurrence.decimalLongitude()),
                        occurrence.datasetName(),
                        null,
                        null,
                        externalId);
                // store the region explicitly using the stateProvince parameter
                observation.setRegion(stateProvince);

                observationRepository.save(observation);
                ingested++;
            }

            if (results.size() < GBIF_PAGE_SIZE) {
                break;
            }

            offset += GBIF_PAGE_SIZE;
            if (offset >= MAX_RECORDS_PER_RUN) {
                break;
            }
        }

        log.info("GBIF ingestion complete: ingested={}, skipped_duplicates={}", ingested, skippedDuplicates);
        return ingested;
    }

    @Transactional
    public int ingestGbifOccurrencesAcrossYears(String country, String stateProvince) {
        int totalIngested = 0;
        int perYearLimit = 100; // cap per year to keep calls small

        for (int year = 2016; year <= 2025; year++) {
            int ingestedThisYear = 0;
            try {
                GbifOccurrenceResponse response = gbifClient.searchOccurrences(country, stateProvince, year,
                        perYearLimit, 0);
                List<GbifOccurrence> results = response != null && response.results() != null ? response.results()
                        : List.of();
                if (results.isEmpty()) {
                    log.info("GBIF {}: year {} returned 0 results", stateProvince, year);
                    continue;
                }

                for (GbifOccurrence occurrence : results) {
                    if (occurrence == null || occurrence.species() == null || occurrence.species().isBlank()) {
                        continue;
                    }

                    Species species = speciesRepository.findByScientificName(occurrence.species())
                            .orElseGet(() -> {
                                Species saved = speciesRepository.save(new Species(
                                        occurrence.species(),
                                        occurrence.vernacularName(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null));
                                speciesSearchRepository.save(SpeciesDocument.fromEntity(saved, stateProvince));
                                return saved;
                            });
                    if (species.getId() != null) {
                        speciesSearchRepository.save(SpeciesDocument.fromEntity(species, stateProvince));
                    }

                    String externalId = occurrence.key() != null ? String.valueOf(occurrence.key()) : null;
                    if (externalId == null
                            || observationRepository.findBySourceAndExternalId("GBIF", externalId).isPresent()) {
                        continue;
                    }

                    Observation observation = new Observation(
                            species,
                            "GBIF",
                            parseDate(occurrence.eventDate()),
                            buildPoint(occurrence.decimalLatitude(), occurrence.decimalLongitude()),
                            occurrence.datasetName(),
                            null,
                            null,
                            externalId);
                    observation.setRegion(stateProvince);
                    observationRepository.save(observation);
                    ingestedThisYear++;
                }

                log.info("GBIF ingestion for {} year {}: ingested={}", stateProvince, year, ingestedThisYear);
                totalIngested += ingestedThisYear;
            } catch (Exception ex) {
                log.warn("GBIF ingestion for {} year {} failed: {}", stateProvince, year, ex.getMessage());
            }
        }

        log.info("GBIF multi-year ingestion complete: totalIngested={}", totalIngested);
        return totalIngested;
    }

    @Transactional
    public int ingestINaturalistObservations(Integer placeId, String region) {
        int ingested = 0;
        int skippedDuplicates = 0;
        int page = 1;

        while (page <= 10) {
            INaturalistObservationResponse response = iNaturalistClient.getObservations(placeId, 30, page);
            List<INaturalistObservation> results = response != null && response.results() != null ? response.results()
                    : List.of();

            if (results.isEmpty()) {
                break;
            }

            for (INaturalistObservation observationDto : results) {
                if (observationDto == null || observationDto.taxonName() == null
                        || observationDto.taxonName().isBlank()) {
                    continue;
                }

                Species species = speciesRepository.findByScientificName(observationDto.taxonName())
                        .orElseGet(() -> {
                            Species saved = speciesRepository.save(new Species(
                                    observationDto.taxonName(),
                                    observationDto.preferredCommonName(),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null));
                            speciesSearchRepository.save(SpeciesDocument.fromEntity(saved, region != null ? region : ""));
                            return saved;
                        });
                if (species.getId() != null) {
                    speciesSearchRepository.save(SpeciesDocument.fromEntity(species, region != null ? region : ""));
                }

                String externalId = observationDto.id() != null ? String.valueOf(observationDto.id()) : null;
                if (externalId == null
                        || observationRepository.findBySourceAndExternalId("INATURALIST", externalId).isPresent()) {
                    skippedDuplicates++;
                    continue;
                }

                Observation observation = new Observation(
                        species,
                        "INATURALIST",
                        parseDate(observationDto.observedOn()),
                        buildPointFromLocationString(observationDto.location()),
                        observationDto.placeGuess(),
                        observationDto.qualityGrade(),
                        observationDto.photoUrl(),
                        externalId);
                // store supplied region (if provided) so we can filter reliably by region
                if (region != null && !region.isBlank()) {
                    observation.setRegion(region);
                }

                observationRepository.save(observation);
                ingested++;
            }

            page++;
        }

        log.info("iNaturalist ingestion complete: ingested={}, skipped_duplicates={}", ingested, skippedDuplicates);
        return ingested;
    }

    @Transactional
    public int backfillRegion(String source, String region) {
        // Delegates to repository modifying query to update NULL regions for the given
        // source
        return observationRepository.backfillRegion(source, region);
    }

    @Transactional
    public int backfillRegionSmart() {
        // Delegates to repository native SQL update which pattern-matches place_name
        // and sets region for GBIF observations.
        return observationRepository.backfillRegionSmart();
    }

    @Transactional
    public int reindexSpeciesToElasticsearch() {
        int indexed = 0;
        for (Species species : speciesRepository.findAll()) {
            String region = observationRepository.findRegionsForSpecies(species.getId()).stream().findFirst().orElse(null);
            speciesSearchRepository.save(SpeciesDocument.fromEntity(species, region));
            indexed++;
        }
        log.info("Reindexed {} species into Elasticsearch", indexed);
        return indexed;
    }

    private Point buildPoint(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private Point buildPointFromLocationString(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }

        String[] values = location.split(",");
        if (values.length < 2) {
            return null;
        }

        try {
            double latitude = Double.parseDouble(values[0].trim());
            double longitude = Double.parseDouble(values[1].trim());
            return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        } catch (NumberFormatException ex) {
            log.warn("Unable to parse location '{}' into a point", location, ex);
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(value.substring(0, 10));
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
