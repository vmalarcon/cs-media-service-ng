package com.expedia.content.media.processing.services.exception;

/**
 * This exception happens when JSON image message cannot be parsed into Java object}
 */
@SuppressWarnings("serial")
public class RequestMessageException extends RuntimeException {
    public RequestMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
