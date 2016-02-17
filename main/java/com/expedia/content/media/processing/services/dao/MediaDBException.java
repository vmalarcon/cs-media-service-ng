package com.expedia.content.media.processing.services.dao;

/**
 * Exception to thrown when a Media DB exception occures.
 */
@SuppressWarnings("serial")
public class MediaDBException extends RuntimeException {

    public MediaDBException(String message, Exception e) {
        super(message, e);
    }

}
