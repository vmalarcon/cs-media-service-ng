package com.expedia.content.media.processing.services.util;

/**
 * This exception happens when JSON image message cannot be parsed into ImageMessage}
 */
@SuppressWarnings("serial")
public class ImageStatusException extends RuntimeException {
    public ImageStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
