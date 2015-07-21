package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;

/**
 * Validates an image message content.
 * rules like: numeric: mediaProviderId, expediaId,categoryId,
 * required: expediaId, fileUrl
 * URL pattern: fileURL, callback
 */
public interface MediaMessageValidator {
    /**
     * Validates the image json message properties.
     * Returns a status indicating if the validation passed or not. A message is included if the validation failed
     *
     * @param imageMessage message to validate
     * @return ValidationStatus contain two validation status, true-successful,
     * false- validation fail , in false case, a validation message is included in ValidationStatus
     */
    ValidationStatus validate(ImageMessage imageMessage) throws Exception;
}
