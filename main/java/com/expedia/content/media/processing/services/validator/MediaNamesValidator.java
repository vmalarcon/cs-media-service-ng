package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.services.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

/**
 * MediaNamesValidator will check the input json message
 * include property 'mediaNames'
 * the value of mediaNames should be array
 * the number of mediaNames should not exceed default setting 50
 */
public class MediaNamesValidator implements MediaStatusValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaNamesValidator.class);
    @Value("${medianame.maximum.count}")
    private int maximumRequestCount;
    /**
     * Validate the object type is as expected.
     *
     * @param message to validate
     * @return ValidationStatus contain two validation status, true-successful,
     * false- validation fail , in false case, a validation message is set in ValidationStatus
     */
    @Override
    public ValidationStatus validate(String message) {
        ValidationStatus validationStatus = new ValidationStatus();
        Map<String, Object> map = JSONUtil.buildMapFromJson(message);
        Object object = map.get("mediaNames");
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
