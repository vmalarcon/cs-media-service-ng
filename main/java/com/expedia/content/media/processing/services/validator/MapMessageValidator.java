package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;

import java.util.List;
import java.util.Map;

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
    List<Map<String, String>> validateImages(List<ImageMessage> messageMapList);

}
