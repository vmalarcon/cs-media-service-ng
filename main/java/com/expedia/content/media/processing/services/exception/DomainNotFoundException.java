package com.expedia.content.media.processing.services.exception;

/**
 * Signals that a Domain was not found
 *
 * @see MediaDomainCategoriesDao
 */
public class DomainNotFoundException extends RuntimeException {
    public DomainNotFoundException(String message) {
        super(message);
    }

    public DomainNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
