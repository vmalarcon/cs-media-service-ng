package com.expedia.content.media.processing.services.validator;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import com.expedia.content.media.processing.services.util.JSONUtil;

/**
 * MediaNamesValidator will check the input json message
 * include property 'mediaNames'
 * the value of mediaNames should be array
 * the number of mediaNames should not exceed default setting
 */
@SuppressWarnings({"PMD.ConfusingTernary"})
public class MediaNamesValidator implements RequestMessageValidator {
    @Value("${medianame.maximum.count}")
    private int maximumRequestCount;
    /**
     * Validate the json is as expected.
     *
     * @param message to validate
     * @return ValidationStatus contain the validation status, {@code true} when successful or
     * {@code false} when the validation fails. When the validation fails a message is also set in the ValidationStatus.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ValidationStatus validate(String message) {
        final ValidationStatus validationStatus = new ValidationStatus();
        final Map<String, Object> map = JSONUtil.buildMapFromJson(message);
        final Object object = map.get("mediaNames");
        if (object == null) {
            validationStatus.setValid(false);
            validationStatus.setMessage("message does not contain property 'messageNames'.");
        } else if (!(object instanceof List)) {
            validationStatus.setValid(false);
            validationStatus.setMessage("messageNames value should be array.");
        } else {
            if (((List) object).isEmpty()) {
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
