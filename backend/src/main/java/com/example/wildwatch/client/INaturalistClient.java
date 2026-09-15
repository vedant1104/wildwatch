package com.example.wildwatch.client;

import com.example.wildwatch.client.dto.INaturalistObservation;
import com.example.wildwatch.client.dto.INaturalistObservationResponse;
import com.example.wildwatch.client.exception.ExternalApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

/**
 * Client for the iNaturalist observations API:
 * https://api.inaturalist.org/v1/observations
 */
@Component
public class INaturalistClient {

        private static final Logger log = LoggerFactory.getLogger(INaturalistClient.class);
        private static final String OBSERVATIONS_PATH = "/v1/observations";

        private final WebClient webClient;

        public INaturalistClient(@Qualifier("inaturalistWebClient") WebClient webClient) {
                this.webClient = webClient;
        }

        @Retry(name = "inaturalist", fallbackMethod = "fallbackGetObservations")
        @CircuitBreaker(name = "inaturalist")
        public INaturalistObservationResponse getObservations(Integer placeId, int perPage, int page) {
                log.debug("iNaturalist getObservations request: placeId={}, perPage={}, page={}", placeId, perPage,
                                page);

                RawResponse raw = webClient.get()
                                .uri(uriBuilder -> uriBuilder.path(OBSERVATIONS_PATH)
                                                .queryParamIfPresent("place_id", Optional.ofNullable(placeId))
                                                .queryParam("per_page", perPage)
                                                .queryParam("page", page)
                                                .build())
                                .retrieve()
                                .bodyToMono(RawResponse.class)
                                .block();

                INaturalistObservationResponse response = toDto(raw);
                log.debug("iNaturalist getObservations response: totalResults={}", response.totalResults());
                return response;
        }

        @SuppressWarnings("unused")
        private INaturalistObservationResponse fallbackGetObservations(Integer placeId, int perPage, int page,
                        Throwable t) {
                log.warn("iNaturalist getObservations failed after retries for placeId={}: {}", placeId,
                                t.getMessage());
                throw new ExternalApiException("Failed to fetch iNaturalist observations for placeId=" + placeId, t);
        }

        private INaturalistObservationResponse toDto(RawResponse raw) {
                if (raw == null) {
                        return new INaturalistObservationResponse(0, 0, 0, List.of());
                }
                List<INaturalistObservation> observations = raw.results() == null
                                ? List.of()
                                : raw.results().stream().map(result -> this.toObservation(result)).toList();
                return new INaturalistObservationResponse(raw.totalResults(), raw.page(), raw.perPage(), observations);
        }

        private INaturalistObservation toObservation(RawResult result) {
                String taxonName = result.taxon() != null ? result.taxon().name() : null;
                String preferredCommonName = result.taxon() != null ? result.taxon().preferredCommonName() : null;
                String photoUrl = (result.photos() != null && !result.photos().isEmpty())
                                ? result.photos().get(0).url()
                                : null;
                return new INaturalistObservation(
                                result.id(),
                                taxonName,
                                preferredCommonName,
                                result.observedOn(),
                                result.location(),
                                result.qualityGrade(),
                                photoUrl,
                                result.placeGuess());
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record RawResponse(
                        @JsonProperty("total_results") int totalResults,
                        int page,
                        @JsonProperty("per_page") int perPage,
                        List<RawResult> results) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record RawResult(
                        Long id,
                        RawTaxon taxon,
                        @JsonProperty("observed_on") String observedOn,
                        String location,
                        @JsonProperty("quality_grade") String qualityGrade,
                        List<RawPhoto> photos,
                        @JsonProperty("place_guess") String placeGuess) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record RawTaxon(
                        String name,
                        @JsonProperty("preferred_common_name") String preferredCommonName) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record RawPhoto(String url) {
        }
}
