package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;

/**
 * Validates an image message.
 */
public interface MediaMessageValidator {
    /**
     * Validates the image message. It doesn't throw an exception and leaves the decision to the caller.
     *
     * @param imageMessage Image to validate
     * @return {@code true} for valid images, {@code false} otherwise
     */
    ValidationStatus validate(ImageMessage imageMessage);
}
