package com.expedia.content.media.processing.services.dao;

/**
 * TODO
 */
@SuppressWarnings("serial")
public class MediaDBException extends RuntimeException {

    public MediaDBException(String message, Exception e) {
        super(message, e);
    }

}
