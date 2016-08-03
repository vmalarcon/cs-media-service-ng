package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;

import com.expedia.content.media.processing.pipeline.reporting.FormattedLogger;
import org.apache.commons.lang.StringUtils;

/**
 * Base implementation of ImageMessage required field validation. Also verifies outer domain fields.
 *
 * @deprecated Use MVELValidator instead
 */
@Deprecated
public class RequiredValidator extends OuterDomainSeekerValidator implements MediaMessageValidator {
    private static final FormattedLogger LOGGER = new FormattedLogger(RequiredValidator.class);

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
        final ValidationStatus validationStatus = new ValidationStatus();

        if (validateFieldNotNullOrNotEmpty(imageMessage)) {
            validationStatus.setValid(true);
        } else {
            validationStatus.setValid(false);
            validationStatus.setMessage(fieldName + " is required.");
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
            LOGGER.info(e, "Required field validation reflection call failed ErrorMessage={}", e.getMessage());
        }
        if ((fieldValue == null || StringUtils.isEmpty(fieldValue.toString())) && imageMessage.getOuterDomainData() != null) {
            fieldValue = seekOuterDomainFields(imageMessage);
        }
        return !(fieldValue == null || StringUtils.isEmpty(fieldValue.toString()));
    }

}
