package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of ImageMessage required field validation. Also verifies outer domain fields.
 */
public class RequiredValidator extends OuterDomainSeekerValidator implements MediaMessageValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequiredValidator.class);

    /**
     * Validates that a specified field exists in the message.
     *
     * @param imageMessage message to validate
     * @return ValidationStatus contains on of two validation statuses, {@code true} if successful,
     *         {@code false} if the validation fails, in the false case, a validation message is
     *         set in ValidationStatus.
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
     * Validates if the field is required or not.
     *
     * @param imageMessage The message containing the field to validate.
     * @return {@code true} if the field exists, {@code false} if the field is not found. 
     */
    private boolean validateFieldNotNullOrNotEmpty(ImageMessage imageMessage) {
        Object fieldValue = null;
        try {
            fieldValue = ReflectionUtil.getFieldValue(imageMessage, fieldName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.info("Required field validation reflection call failed! error=[{}]", e.getMessage(), e);
        }
        if ((fieldValue == null || StringUtils.isEmpty(fieldValue.toString())) && imageMessage.getOuterDomainDataList() != null) {
            fieldValue = seekOuterDomainFields(imageMessage);
        }
        if (fieldValue == null || StringUtils.isEmpty(fieldValue.toString())) {
            return false;
        }
        return true;
    }

}
