package com.example.wildwatch.client;

import com.example.wildwatch.client.dto.INaturalistObservation;
import com.example.wildwatch.client.dto.INaturalistObservationResponse;
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

@SpringBootTest(classes = INaturalistClientTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class INaturalistClientTest {

    private static final String FIXTURES = "src/test/resources/wiremock/inaturalist/";

    private static WireMockServer wireMockServer;

    @Autowired
    private INaturalistClient inaturalistClient;

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
        registry.add("wildwatch.inaturalist.base-url", () -> "http://localhost:" + wireMockServer.port() + "/v1");
        registry.add("resilience4j.retry.instances.inaturalist.max-attempts", () -> 3);
        registry.add("resilience4j.retry.instances.inaturalist.wait-duration", () -> "10ms");
        registry.add("resilience4j.retry.instances.inaturalist.enable-exponential-backoff", () -> true);
        registry.add("resilience4j.retry.instances.inaturalist.exponential-backoff-multiplier", () -> 2);
        registry.add("resilience4j.retry.instances.inaturalist.retry-exceptions",
                () -> "org.springframework.web.reactive.function.client.WebClientResponseException");
        registry.add("resilience4j.circuitbreaker.instances.inaturalist.sliding-window-size", () -> 10);
        registry.add("resilience4j.circuitbreaker.instances.inaturalist.minimum-number-of-calls", () -> 10);
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
    void getObservations_mapsRealINaturalistResponseShape() throws Exception {
        String body = readFixture("observations-response.json");
        wireMockServer.stubFor(get(urlPathEqualTo("/v1/observations"))
                .withQueryParam("place_id", WireMock.equalTo("7"))
                .withQueryParam("per_page", WireMock.equalTo("30"))
                .withQueryParam("page", WireMock.equalTo("1"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));

        INaturalistObservationResponse response = inaturalistClient.getObservations(7, 30, 1);

        assertThat(response.totalResults()).isEqualTo(2);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.perPage()).isEqualTo(30);
        assertThat(response.results()).hasSize(2);

        INaturalistObservation tiger = response.results().get(0);
        assertThat(tiger.taxonName()).isEqualTo("Panthera tigris");
        assertThat(tiger.preferredCommonName()).isEqualTo("Tiger");
        assertThat(tiger.observedOn()).isEqualTo("2023-06-14");
        assertThat(tiger.location()).isEqualTo("20.5937,78.9629");
        assertThat(tiger.qualityGrade()).isEqualTo("research");
        assertThat(tiger.photoUrl()).isEqualTo("https://static.inaturalist.org/photos/1001/square.jpg");
        assertThat(tiger.placeGuess()).isEqualTo("Corbett National Park, Uttarakhand, India");

        INaturalistObservation elephant = response.results().get(1);
        assertThat(elephant.taxonName()).isEqualTo("Elephas maximus");
        assertThat(elephant.photoUrl()).isNull();
    }

    @Test
    void getObservations_retriesOnServerErrorThenSucceeds() throws Exception {
        String body = readFixture("observations-response.json");

        wireMockServer.stubFor(get(urlPathEqualTo("/v1/observations"))
                .inScenario("inaturalist-retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-attempt"));

        wireMockServer.stubFor(get(urlPathEqualTo("/v1/observations"))
                .inScenario("inaturalist-retry")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));

        INaturalistObservationResponse response = inaturalistClient.getObservations(7, 30, 1);

        assertThat(response.totalResults()).isEqualTo(2);
        wireMockServer.verify(2, getRequestedFor(urlPathEqualTo("/v1/observations")));
    }

    @Test
    void getObservations_throwsExternalApiExceptionWhenRetriesExhausted() {
        wireMockServer.stubFor(get(urlPathEqualTo("/v1/observations"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> inaturalistClient.getObservations(7, 30, 1))
                .isInstanceOf(ExternalApiException.class);

        wireMockServer.verify(3, getRequestedFor(urlPathEqualTo("/v1/observations")));
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
    @Import({WebClientConfig.class, ResilienceEventLoggingConfig.class, INaturalistClient.class})
    static class TestConfig {
    }
}
