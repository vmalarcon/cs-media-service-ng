package com.expedia.content.media.processing.services.reqres;


import lombok.Builder;
import lombok.Getter;


/**
 * Represents a Temporary Derivative Message sent to media/v1/tempderivative
 */
@Builder
@Getter
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class TempDerivativeMessage {
    private final String fileUrl;
    private final String rotation;
    private final Integer width;
    private final Integer height;
}