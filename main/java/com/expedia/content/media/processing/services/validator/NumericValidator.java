package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

/**
 * Base implementation of ImageMessage numeric field validation. Also verifies outer domain fields.
 * If the fields doesn't exist the validation passes.
 *
 * @deprecated Use MVELValidator instead
 */
@Deprecated
public class NumericValidator extends OuterDomainSeekerValidator implements MediaMessageValidator {
    private static final FormattedLogger LOGGER = new FormattedLogger(NumericValidator.class);
    
    /**
     * Validates if a specified field is a number.
     *
     * @param imageMessage message to validate.
     * @return ValidationStatus contains on of two validation statuses, {@code true} if successful,
     *         {@code false} if the validation fails, in the false case, a validation message is
     *         set in ValidationStatus.
     */
    @Override
    public ValidationStatus validate(ImageMessage imageMessage) {
        final ValidationStatus validationStatus = new ValidationStatus();
        
        if (validateFieldNumeric(imageMessage)) {
            validationStatus.setValid(true);
        } else {
            validationStatus.setValid(false);
            validationStatus.setMessage(fieldName + " is not numeric.");
        }
        return validationStatus;
    }
    
    /**
     * Validates if the field value is a number or not.
     *
     * @param imageMessage The message containing the field to validate.
     * @return {@code true} if successful or doesn't exist, {@code false} if the field is not numeric. 
     */
    private boolean validateFieldNumeric(ImageMessage imageMessage) {
        Object fieldValue = null;
        try {
            fieldValue = ReflectionUtil.getFieldValue(imageMessage, fieldName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.info(e, "Numeric field validation reflection call failed ErrorMessage={}", Arrays.asList(e.getMessage()), imageMessage);
        }
        if (fieldValue == null && imageMessage.getOuterDomainData() != null) {
            fieldValue = seekOuterDomainFields(imageMessage);
        }
        return !(fieldValue != null && !StringUtils.isNumeric(fieldValue.toString()));
    }
    
}
