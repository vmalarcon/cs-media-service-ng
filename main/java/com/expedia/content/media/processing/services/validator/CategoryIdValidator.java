package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;

/**
 * Created by seli on 2015-07-14.
 */
public class CategoryIdValidator extends IsNumbericValidator {

    @Override
    public ValidationStatus validate(ImageMessage image) {
        return super.validate(image);
    }
}
