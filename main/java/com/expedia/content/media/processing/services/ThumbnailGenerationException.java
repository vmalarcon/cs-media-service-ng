package com.expedia.content.media.processing.services;


public class ThumbnailGenerationException extends RuntimeException {
    public ThumbnailGenerationException(String message, Exception cause) {
        super(message, cause);
    }
}
