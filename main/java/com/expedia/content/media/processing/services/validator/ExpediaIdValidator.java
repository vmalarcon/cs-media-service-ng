package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by seli on 2015-07-13.
 */
public class ExpediaIdValidator extends IsNumbericValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpediaIdValidator.class);

    @Override
    public ValidationStatus validate(ImageMessage image) {
        if (image.getExpediaId() == null) {
            ValidationStatus validationStatus = new ValidationStatus();
            validationStatus.setStatus(false);
            validationStatus.setMessage("expediaId is required.");
            LOGGER.debug("expediaId is missed");
        }
        return super.validate(image);
    }
}
