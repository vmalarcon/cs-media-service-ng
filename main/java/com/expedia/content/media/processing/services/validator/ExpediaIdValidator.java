package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.domain.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

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
        if (image.getExpediaId() == null && (ImageType.LODGING.equals(image.getImageType())
                || ImageType.VT.equals(image.getImageType()))) {
            String errorMsg = "expediaId is required when imageType is {0}.";
            ValidationStatus validationStatus = new ValidationStatus();
            validationStatus.setValid(false);
            errorMsg = MessageFormat.format(errorMsg, image.getImageType());
            validationStatus.setMessage(errorMsg);
            LOGGER.debug(errorMsg);
            return validationStatus;
        }
        return super.validate(image);
    }
}
