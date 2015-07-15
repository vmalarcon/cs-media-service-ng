package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExpediaIdValidator will check whether expediaId is a number or is missed
 */
public class ExpediaIdValidator extends NumericValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpediaIdValidator.class);

    /**
     * validate whether the expediaId  missed or is a number
     *
     * @param image
     * @return ValidationStatus contain two validation status, true-successful,
     * false- validation fail , in false case, a validation message is set in ValidationStatus
     */
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
