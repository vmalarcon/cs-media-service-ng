package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

/**
 * Base implementation of ImageMessage required field validation
 */
public class RequiredValidator implements MediaMessageValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequiredValidator.class);
    private String fieldName;

    /**
     * Validate the specified field exists in message.
     *
     * @param imageMessage message to validate
     * @return ValidationStatus contain two validation status, true-successful,
     * false- validation fail , in false case, a validation message is set in ValidationStatus
     */
    @Override
    public ValidationStatus validate(ImageMessage imageMessage) {
        ValidationStatus validationStatus = new ValidationStatus();

        if (!validateFieldNotNullOrNotEmpty(imageMessage)) {
            validationStatus.setValid(false);
            validationStatus.setMessage(fieldName + " is required.");
        } else {
            validationStatus.setValid(true);
        }
        return validationStatus;
    }

    /**
     * validate the field value is a number or not
     *
     * @param imageMessage
     * @return boolean
     */
    private boolean validateFieldNotNullOrNotEmpty(ImageMessage imageMessage) {
        Object fieldValue = null;
        try {
            fieldValue = ReflectionUtil.getFieldValue(imageMessage, fieldName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("reflection call failed : error=[{}]", e);
            throw new NoSuchElementException(fieldName + " does not exist, please check configuration file");
        }
        if (fieldValue == null || StringUtils.isEmpty(fieldValue.toString())) {
            return false;
        }
        return true;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
