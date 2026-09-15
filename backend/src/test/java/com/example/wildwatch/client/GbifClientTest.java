package com.example.wildwatch.client;

import com.example.wildwatch.client.dto.GbifOccurrence;
import com.example.wildwatch.client.dto.GbifOccurrenceResponse;
import com.example.wildwatch.client.exception.ExternalApiException;
import com.example.wildwatch.config.ResilienceEventLoggingConfig;
import com.example.wildwatch.config.WebClientConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = GbifClientTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class GbifClientTest {

    private static final String FIXTURES = "src/test/resources/wiremock/gbif/";

    private static WireMockServer wireMockServer;

    @Autowired
    private GbifClient gbifClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("wildwatch.gbif.base-url", () -> "http://localhost:" + wireMockServer.port() + "/v1");
        registry.add("resilience4j.retry.instances.gbif.max-attempts", () -> 3);
        registry.add("resilience4j.retry.instances.gbif.wait-duration", () -> "10ms");
        registry.add("resilience4j.retry.instances.gbif.enable-exponential-backoff", () -> true);
        registry.add("resilience4j.retry.instances.gbif.exponential-backoff-multiplier", () -> 2);
        registry.add("resilience4j.retry.instances.gbif.retry-exceptions",
                () -> "org.springframework.web.reactive.function.client.WebClientResponseException");
        registry.add("resilience4j.circuitbreaker.instances.gbif.sliding-window-size", () -> 10);
        registry.add("resilience4j.circuitbreaker.instances.gbif.minimum-number-of-calls", () -> 10);
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @AfterEach
    void verifyNoUnmatchedRequests() {
        assertThat(wireMockServer.findAllUnmatchedRequests()).isEmpty();
    }

    @Test
    void searchOccurrences_mapsRealGbifResponseShape() throws Exception {
        String body = readFixture("occurrence-search-response.json");
        wireMockServer.stubFor(get(urlPathEqualTo("/v1/occurrence/search"))
                .withQueryParam("country", WireMock.equalTo("IN"))
                .withQueryParam("stateProvince", WireMock.equalTo("Uttarakhand"))
                .withQueryParam("limit", WireMock.equalTo("20"))
                .withQueryParam("offset", WireMock.equalTo("0"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));

        GbifOccurrenceResponse response = gbifClient.searchOccurrences("IN", "Uttarakhand", 20, 0);

        assertThat(response.count()).isEqualTo(2);
        assertThat(response.offset()).isEqualTo(0);
        assertThat(response.limit()).isEqualTo(20);
        assertThat(response.results()).hasSize(2);

        GbifOccurrence tiger = response.results().get(0);
        assertThat(tiger.species()).isEqualTo("Panthera tigris");
        assertThat(tiger.vernacularName()).isEqualTo("Tiger");
        assertThat(tiger.eventDate()).isEqualTo("2023-06-14T09:30:00");
        assertThat(tiger.decimalLatitude()).isEqualTo(20.5937);
        assertThat(tiger.decimalLongitude()).isEqualTo(78.9629);
        assertThat(tiger.basisOfRecord()).isEqualTo("HUMAN_OBSERVATION");
        assertThat(tiger.datasetName()).isEqualTo("iNaturalist research-grade observations");
        assertThat(tiger.year()).isEqualTo(2023);
    }

    @Test
    void getOccurrenceCountByYear_extractsRequestedYearFromFacets() throws Exception {
        String body = readFixture("occurrence-year-facet-response.json");
        wireMockServer.stubFor(get(urlPathEqualTo("/v1/occurrence/search"))
                .withQueryParam("facet", WireMock.equalTo("year"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));

        long count2023 = gbifClient.getOccurrenceCountByYear("Panthera tigris", "Uttarakhand", 2023);
        long countMissingYear = gbifClient.getOccurrenceCountByYear("Panthera tigris", "Uttarakhand", 1999);

        assertThat(count2023).isEqualTo(78);
        assertThat(countMissingYear).isEqualTo(0);
    }

    @Test
    void searchOccurrences_retriesOnServerErrorThenSucceeds() throws Exception {
        String body = readFixture("occurrence-search-response.json");

        wireMockServer.stubFor(get(urlPathEqualTo("/v1/occurrence/search"))
                .inScenario("gbif-retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second-attempt"));

        wireMockServer.stubFor(get(urlPathEqualTo("/v1/occurrence/search"))
                .inScenario("gbif-retry")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("third-attempt"));

        wireMockServer.stubFor(get(urlPathEqualTo("/v1/occurrence/search"))
                .inScenario("gbif-retry")
                .whenScenarioStateIs("third-attempt")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));

        GbifOccurrenceResponse response = gbifClient.searchOccurrences("IN", "Uttarakhand", 20, 0);

        assertThat(response.count()).isEqualTo(2);
        wireMockServer.verify(3, getRequestedFor(urlPathEqualTo("/v1/occurrence/search")));
    }

    @Test
    void searchOccurrences_throwsExternalApiExceptionWhenRetriesExhausted() {
        wireMockServer.stubFor(get(urlPathEqualTo("/v1/occurrence/search"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> gbifClient.searchOccurrences("IN", "Uttarakhand", 20, 0))
                .isInstanceOf(ExternalApiException.class);

        wireMockServer.verify(3, getRequestedFor(urlPathEqualTo("/v1/occurrence/search")));
    }

    private static String readFixture(String name) throws Exception {
        return Files.readString(Path.of(FIXTURES + name));
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            QuartzAutoConfiguration.class,
            DataRedisAutoConfiguration.class,
            DataRedisReactiveAutoConfiguration.class,
            SessionAutoConfiguration.class
    })
    @Import({WebClientConfig.class, ResilienceEventLoggingConfig.class, GbifClient.class})
    static class TestConfig {
    }
}
