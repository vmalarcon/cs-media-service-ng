package com.expedia.content.media.processing.services.validator;

import java.util.List;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;

/**
 * Validate requested json message that are mapped to ImageMessage
 */
public interface MapMessageValidator {
    /**
     * Validates ImageMessage list.
     *
     * @param messageMapList ImageMessage list to validate
     * @return Map list, with attribute fileName and error description
     */
    List<String> validateImages(List<ImageMessage> messageMapList);

}
