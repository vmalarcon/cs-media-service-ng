package com.expedia.content.media.processing.services.validator;


/**
 * Validates an media status request content.
 * required: mediaNames
 */
public interface MediaStatusValidator {
    /**
     * Validates the image json message properties.
     * Returns a status indicating if the validation passed or not. A message is included if the validation failed
     *
     * @param  message to validate
     * @return ValidationStatus contain two validation status, true-successful,
     * false- validation fail , in false case, a validation message is included in ValidationStatus
     */
    ValidationStatus validate(String message) throws Exception;
}
