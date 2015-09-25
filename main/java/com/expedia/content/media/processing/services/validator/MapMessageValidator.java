package com.expedia.content.media.processing.services.validator;

import java.util.List;
import java.util.Map;

/**
 * Validate requested json message that not mapped to ImageMessage
 */
public interface MapMessageValidator {
    /**
     * Validates the image json message properties.
     * Returns a status indicating if the validation passed or not. A message is included if the validation failed
     *
     * @param messageMapList json message to validate
     * @return ValidationStatus contain the validation status, {@code true} when successful or
     * {@code false} when the validation fails. When the validation fails a message is also set in the ValidationStatus.
     */
    List<Map<String, String>> validate(List<Map<String, Object>> messageMapList) throws Exception;
}
