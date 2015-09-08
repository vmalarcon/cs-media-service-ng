package com.expedia.content.media.processing.services.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class MediaNamesValidator implements MediaMessageValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaNamesValidator.class);
    @Value("${medianame.maximum.count}")
    private int maximumRequestCount;
    /**
     * Validate the object type is as expected.
     *
     * @param object to validate
     * @return ValidationStatus contain two validation status, true-successful,
     * false- validation fail , in false case, a validation message is set in ValidationStatus
     */
    @Override
    public ValidationStatus validate(Object object) {
        ValidationStatus validationStatus = new ValidationStatus();
        if (object == null) {
            validationStatus.setValid(false);
            validationStatus.setMessage("message does not contain property 'messageNames'.");
        } else if (!(object instanceof List)) {
            validationStatus.setValid(false);
            validationStatus.setMessage("messageNames value should be array.");
        } else {
            if (((List) object).size() <= 0) {
                validationStatus.setMessage("messageNames value is required.");
                return validationStatus;
            }
            if (((List) object).size() > maximumRequestCount) {
                validationStatus.setMessage("messageNames count exceed the maximum " + maximumRequestCount);
                return validationStatus;
            }
            validationStatus.setValid(true);
        }
        return validationStatus;
    }

}
