package com.example.wildwatch.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

        @Bean
        public WebClient gbifWebClient(
                        WebClient.Builder builder,
                        @Value("${wildwatch.webclient.connect-timeout-ms:5000}") int connectTimeoutMs,
                        @Value("${wildwatch.webclient.read-timeout-ms:10000}") int readTimeoutMs) {
                return builder
                                .baseUrl("https://api.gbif.org")
                                .exchangeStrategies(ExchangeStrategies.builder()
                                                .codecs(configurer -> configurer.defaultCodecs()
                                                                .maxInMemorySize(2 * 1024 * 1024))
                                                .build())
                                .clientConnector(new ReactorClientHttpConnector(
                                                buildHttpClient(connectTimeoutMs, readTimeoutMs)))
                                .build();
        }

        @Bean
        public WebClient inaturalistWebClient(
                        WebClient.Builder builder,
                        @Value("${wildwatch.webclient.connect-timeout-ms:5000}") int connectTimeoutMs,
                        @Value("${wildwatch.webclient.read-timeout-ms:10000}") int readTimeoutMs) {
                return builder
                                .baseUrl("https://api.inaturalist.org")
                                .exchangeStrategies(ExchangeStrategies.builder()
                                                .codecs(configurer -> configurer.defaultCodecs()
                                                                .maxInMemorySize(2 * 1024 * 1024))
                                                .build())
                                .clientConnector(new ReactorClientHttpConnector(
                                                buildHttpClient(connectTimeoutMs, readTimeoutMs)))
                                .build();
        }

        private HttpClient buildHttpClient(int connectTimeoutMs, int readTimeoutMs) {
                return HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                                .responseTimeout(Duration.ofMillis(readTimeoutMs))
                                .doOnConnected(connection -> connection
                                                .addHandlerLast(new ReadTimeoutHandler(readTimeoutMs,
                                                                TimeUnit.MILLISECONDS))
                                                .addHandlerLast(new WriteTimeoutHandler(readTimeoutMs,
                                                                TimeUnit.MILLISECONDS)));
        }
}