package com.expedia.content.media.processing.services.dao;

/**
 * Signals that a Domain was not found
 *
 * @see MediaDomainCategoriesDao
 */
public class CatalogItemmediaNotFoundException extends RuntimeException {
    public CatalogItemmediaNotFoundException(String message) {
        super(message);
    }

    public CatalogItemmediaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
