package com.expedia.content.media.processing.services;


/**
 * class to represent any exception that occurs during thumbnail generation
 */
public class ThumbnailGenerationException extends RuntimeException {
    public ThumbnailGenerationException(String message, Exception cause) {
        super(message, cause);
    }
}
