package com.expedia.content.media.processing.services.exception;

/**
 * This exception is thrown when pagination parameters are wrong
 */

public class PaginationValidationException extends RuntimeException {
    public PaginationValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaginationValidationException(String message) {
        super(message);
    }
}
