package com.expedia.content.media.processing.services.validator;

import java.util.List;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;

/**
 * Provided the validation logic for MediaUpdate.
 *
 */
public class LodgingUpdateValidator extends LodgingValidator {    
    public List<String> validateImages(List<ImageMessage> messageList) {
         return super.validateImages(messageList);
    }
}
