package com.expedia.content.media.processing.services.exception;

/**
 * Signals that a Domain was not found
 *
 * @see com.expedia.content.media.processing.services.dao.DomainCategoriesDao
 */
public class DomainNotFoundException extends RuntimeException {
    public DomainNotFoundException(String message) {
        super(message);
    }
}
