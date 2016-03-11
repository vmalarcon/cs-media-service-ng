package com.expedia.content.media.processing.services.reqres;


import lombok.Data;


/**
 * Represents a Temporary Derivative Message sent to media/v1/tempderivative
 */
@SuppressWarnings({"PMD.UnusedPrivateField"})
public @Data class TempDerivativeMessage {
    private final String fileUrl;
    private final String rotation;
    private final Integer width;
    private final Integer height;
}
