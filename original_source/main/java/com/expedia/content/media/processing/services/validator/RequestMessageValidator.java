package com.expedia.content.media.processing.services.validator;


/**
 * Validate requested json message that not mapped to ImageMessage
 */
public interface RequestMessageValidator {
    /**
     * Validates the image json message properties.
     * Returns a status indicating if the validation passed or not. A message is included if the validation failed
     *
     * @param  message to validate
     * @return ValidationStatus contain the validation status, {@code true} when successful or
     * {@code false} when the validation fails. When the validation fails a message is also set in the ValidationStatus.
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    ValidationStatus validate(String message) throws Exception;
}
