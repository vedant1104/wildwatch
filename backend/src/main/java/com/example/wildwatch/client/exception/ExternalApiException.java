package com.example.wildwatch.client.exception;

/**
 * Thrown when a call to an external API fails after all retry and circuit
 * breaker handling has been exhausted, so callers never need to catch raw
 * WebClient exceptions.
 */
public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalApiException(String message) {
        super(message);
    }
}
