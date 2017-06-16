package com.expedia.content.media.processing.services.exception;

/**
 * Signals that a Media was not found
 *
 * @see com.expedia.content.media.processing.services.dao.MediaDao
 */
public class MediaNotFoundException extends RuntimeException {
    public MediaNotFoundException(String message) {
        super(message);
    }

    public MediaNotFoundException() {
        super();
    }
}
