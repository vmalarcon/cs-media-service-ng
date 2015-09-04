package com.expedia.content.media.processing.services.validator;

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
     * @param object message to validate
     * @return ValidationStatus contain two validation status, true-successful,
     * false- validation fail , in false case, a validation message is set in ValidationStatus
     */
    @Override
    public ValidationStatus validate(Object object) {
        ValidationStatus validationStatus = new ValidationStatus();

        if (!validateFieldNotNullOrNotEmpty(object)) {
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
     * @param object
     * @return boolean
     */
    private boolean validateFieldNotNullOrNotEmpty(Object object) {
        Object fieldValue = null;
        try {
            fieldValue = ReflectionUtil.getFieldValue(object, fieldName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("reflection call fail", e);
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
