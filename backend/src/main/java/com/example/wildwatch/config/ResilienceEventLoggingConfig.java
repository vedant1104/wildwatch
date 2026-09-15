package com.example.wildwatch.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Registers WARN-level logging for every Resilience4j retry attempt, circuit
 * breaker error and state transition, for every retry/circuit breaker
 * instance configured in application.properties (present and future).
 */
@Configuration
public class ResilienceEventLoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceEventLoggingConfig.class);

    public ResilienceEventLoggingConfig(RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        retryRegistry.getAllRetries().forEach(this::registerRetryLogging);
        retryRegistry.getEventPublisher().onEntryAdded(event -> registerRetryLogging(event.getAddedEntry()));

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerCircuitBreakerLogging);
        circuitBreakerRegistry.getEventPublisher().onEntryAdded(event -> registerCircuitBreakerLogging(event.getAddedEntry()));
    }

    private void registerRetryLogging(Retry retry) {
        retry.getEventPublisher().onRetry(event -> log.warn(
                "Retry attempt #{} for '{}' after failure: {}",
                event.getNumberOfRetryAttempts(), retry.getName(),
                event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "unknown"));
    }

    private void registerCircuitBreakerLogging(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onError(event -> log.warn(
                        "Circuit breaker '{}' recorded a call error: {}",
                        circuitBreaker.getName(), event.getThrowable().getMessage()))
                .onStateTransition(event -> log.warn(
                        "Circuit breaker '{}' transitioned {}",
                        circuitBreaker.getName(), event.getStateTransition()));
    }
}
