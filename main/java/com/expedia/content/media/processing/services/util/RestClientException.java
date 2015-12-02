package com.expedia.content.media.processing.services.util;

/**
 * Generic wrapper for MediaServiceClient exceptions
 *
 * @see ConfigRestClient
 */
public class RestClientException extends RuntimeException {
    public RestClientException(String message) {
        super(message);
    }

    public RestClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestClientException(Throwable cause) {
        super(cause);
    }
}
