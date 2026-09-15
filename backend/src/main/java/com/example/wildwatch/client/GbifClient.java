package com.example.wildwatch.client;

import com.example.wildwatch.client.dto.GbifOccurrenceResponse;
import com.example.wildwatch.client.exception.ExternalApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

/**
 * Client for the GBIF (Global Biodiversity Information Facility) occurrence
 * search API: https://api.gbif.org/v1/occurrence/search
 */
@Component
public class GbifClient {

        private static final Logger log = LoggerFactory.getLogger(GbifClient.class);
        private static final String SEARCH_PATH = "/v1/occurrence/search";
        private static final int YEAR_FACET_LIMIT = 200;

        private final WebClient webClient;

        public GbifClient(@Qualifier("gbifWebClient") WebClient webClient) {
                this.webClient = webClient;
        }

        @Retry(name = "gbif", fallbackMethod = "fallbackSearchOccurrences")
        @CircuitBreaker(name = "gbif")
        public GbifOccurrenceResponse searchOccurrences(String country, String stateProvince, int limit, int offset) {
                log.debug("GBIF searchOccurrences request: country={}, stateProvince={}, limit={}, offset={}",
                                country, stateProvince, limit, offset);

                GbifOccurrenceResponse response = webClient.get()
                                .uri(uriBuilder -> uriBuilder.path(SEARCH_PATH)
                                                .queryParamIfPresent("country", Optional.ofNullable(country))
                                                .queryParamIfPresent("stateProvince",
                                                                Optional.ofNullable(stateProvince))
                                                .queryParam("limit", limit)
                                                .queryParam("offset", offset)
                                                .build())
                                .retrieve()
                                .bodyToMono(GbifOccurrenceResponse.class)
                                .block();

                log.debug("GBIF searchOccurrences response: count={}", response != null ? response.count() : null);
                return response;
        }

        @Retry(name = "gbif", fallbackMethod = "fallbackSearchOccurrencesWithYear")
        @CircuitBreaker(name = "gbif")
        public GbifOccurrenceResponse searchOccurrences(String country, String stateProvince, Integer year, int limit,
                        int offset) {
                log.debug("GBIF searchOccurrences request: country={}, stateProvince={}, year={}, limit={}, offset={}",
                                country, stateProvince, year, limit, offset);

                GbifOccurrenceResponse response = webClient.get()
                                .uri(uriBuilder -> {
                                        var b = uriBuilder.path(SEARCH_PATH)
                                                        .queryParamIfPresent("country", Optional.ofNullable(country))
                                                        .queryParamIfPresent("stateProvince",
                                                                        Optional.ofNullable(stateProvince))
                                                        .queryParam("limit", limit)
                                                        .queryParam("offset", offset);
                                        if (year != null) {
                                                b = b.queryParam("year", year);
                                        }
                                        return b.build();
                                })
                                .retrieve()
                                .bodyToMono(GbifOccurrenceResponse.class)
                                .block();

                log.debug("GBIF searchOccurrences response: count={}", response != null ? response.count() : null);
                return response;
        }

        /**
         * Fetches the occurrence count for a given year by requesting a year
         * facet breakdown (scoped to the given species/state) and pulling out
         * the bucket matching {@code year}. Calling this once per year of
         * interest builds the series for a trend chart.
         */
        @Retry(name = "gbif", fallbackMethod = "fallbackGetOccurrenceCountByYear")
        @CircuitBreaker(name = "gbif")
        public long getOccurrenceCountByYear(String scientificName, String stateProvince, int year) {
                log.debug("GBIF getOccurrenceCountByYear request: scientificName={}, stateProvince={}, year={}",
                                scientificName, stateProvince, year);

                GbifOccurrenceResponse response = webClient.get()
                                .uri(uriBuilder -> uriBuilder.path(SEARCH_PATH)
                                                .queryParamIfPresent("scientificName",
                                                                Optional.ofNullable(scientificName))
                                                .queryParamIfPresent("stateProvince",
                                                                Optional.ofNullable(stateProvince))
                                                .queryParam("limit", 0)
                                                .queryParam("facet", "year")
                                                .queryParam("facetLimit", YEAR_FACET_LIMIT)
                                                .build())
                                .retrieve()
                                .bodyToMono(GbifOccurrenceResponse.class)
                                .block();

                long count = extractYearCount(response, year);
                log.debug("GBIF getOccurrenceCountByYear response: year={}, count={}", year, count);
                return count;
        }

        private long extractYearCount(GbifOccurrenceResponse response, int year) {
                if (response == null || response.facets() == null) {
                        return 0L;
                }
                String yearAsString = String.valueOf(year);
                return response.facets().stream()
                                .filter(facet -> "YEAR".equalsIgnoreCase(facet.field()))
                                .flatMap(facet -> facet.counts().stream())
                                .filter(facetCount -> yearAsString.equals(facetCount.name()))
                                .mapToLong(facetCount -> facetCount == null ? 0L : facetCount.count())
                                .findFirst()
                                .orElse(0L);
        }

        @SuppressWarnings("unused")
        private GbifOccurrenceResponse fallbackSearchOccurrences(String country, String stateProvince, int limit,
                        int offset, Throwable t) {
                log.warn("GBIF searchOccurrences failed after retries for country={}, stateProvince={}: {}",
                                country, stateProvince, t.getMessage());
                throw new ExternalApiException(
                                "Failed to fetch GBIF occurrences for country=" + country + ", stateProvince="
                                                + stateProvince,
                                t);
        }

        @SuppressWarnings("unused")
        private GbifOccurrenceResponse fallbackSearchOccurrencesWithYear(String country, String stateProvince,
                        Integer year, int limit, int offset, Throwable t) {
                log.warn("GBIF searchOccurrences failed after retries for country={}, stateProvince={}, year={}: {}",
                                country, stateProvince, year, t.getMessage());
                throw new ExternalApiException(
                                "Failed to fetch GBIF occurrences for country=" + country + ", stateProvince="
                                                + stateProvince + ", year=" + year,
                                t);
        }

        @SuppressWarnings("unused")
        private long fallbackGetOccurrenceCountByYear(String scientificName, String stateProvince, int year,
                        Throwable t) {
                log.warn("GBIF getOccurrenceCountByYear failed after retries for scientificName={}, stateProvince={}, year={}: {}",
                                scientificName, stateProvince, year, t.getMessage());
                throw new ExternalApiException(
                                "Failed to fetch GBIF occurrence count for scientificName=" + scientificName + ", year="
                                                + year,
                                t);
        }
}
