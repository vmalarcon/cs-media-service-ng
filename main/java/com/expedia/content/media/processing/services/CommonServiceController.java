package com.expedia.content.media.processing.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.expedia.content.media.processing.services.util.JSONUtil;

import expedia.content.solutions.metrics.annotations.Counter;

/**
 * Common functionality shared by the service controllers.
 */
@SuppressWarnings({"PMD.AbstractClassWithoutAbstractMethod"})
public abstract class CommonServiceController {

    protected static final String REQUEST_ID = "request-id";
    protected static final String VALID_MESSAGE = "OK";
    protected static final String INVALID_MESSAGE = "Invalid Message.";

    /**
     * Builds an error response.
     *
     * @param errorMessage Failed message from validate.
     * @param resourcePath Path of the accessed resource. 
     * @param httpStatus HTTP status of the error.
     * @return A response with an error code.
     */
    @Counter(name = "badRequestCounter")
    public ResponseEntity<String> buildErrorResponse(final String errorMessage, final String resourcePath, final HttpStatus httpStatus) {
        final String responseMessage = JSONUtil.generateJsonForErrorResponse(errorMessage, resourcePath, httpStatus.value(), httpStatus.getReasonPhrase());
        return new ResponseEntity<>(responseMessage, httpStatus);
    }

    /**
     * Pulls the request id from the header values.
     * 
     * @param headers Header value map.
     * @return The request id
     */
    protected String getRequestId(MultiValueMap<String, String> headers) {
        return headers.getFirst(REQUEST_ID);
    }

}
