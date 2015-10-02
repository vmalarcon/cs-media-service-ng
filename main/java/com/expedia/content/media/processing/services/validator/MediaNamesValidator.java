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
 * the number of mediaNames should not exceed default setting
 */
public class MediaNamesValidator implements RequestMessageValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaNamesValidator.class);
    @Value("${medianame.maximum.count}")
    private int maximumRequestCount;
    /**
     * Validate the json is as expected.
     *
     * @param message to validate
     * @return ValidationStatus contain the validation status, {@code true} when successful or
     * {@code false} when the validation fails. When the validation fails a message is also set in the ValidationStatus.
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
                validationStatus.setValid(false);
                validationStatus.setMessage("messageNames value is required.");
                return validationStatus;
            }
            if (((List) object).size() > maximumRequestCount) {
                validationStatus.setValid(false);
                validationStatus.setMessage("messageNames count exceed the maximum " + maximumRequestCount);
                return validationStatus;
            }
            validationStatus.setValid(true);
        }
        return validationStatus;
    }

}
