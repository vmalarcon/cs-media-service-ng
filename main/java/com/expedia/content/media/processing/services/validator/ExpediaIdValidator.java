package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;

import java.text.MessageFormat;
import java.util.Arrays;

/**
 * ExpediaIdValidator will check whether expediaId is a number or is missing.
 *
 * @deprecated Use MVELValidator instead
 */
@Deprecated
public class ExpediaIdValidator extends NumericValidator {
    
    private static final FormattedLogger LOGGER = new FormattedLogger(ExpediaIdValidator.class);
    
    /**
     * Validate whether the expediaId missed or is a number. Ignores the validation if the image type
     * isn't lodging or VT.
     *
     * @param image ImageMessage to validate.
     * @return ValidationStatus contain two validation status, {@code true} if successful,
     *         {@code false} if the validation fail, in the false case, a validation message is set in ValidationStatus.
     */
    @Override
    public ValidationStatus validate(ImageMessage image) {
        if (image.getOuterDomainData().getDomain() == Domain.LODGING) {
            final Object eidValue = findEidValue(image);
            if (eidValue == null && image.getExpediaId() == null) {
                return buildFailedValidationStatus(image);
            }
            return super.validate(image);
        }
        final ValidationStatus validationStatus = new ValidationStatus();
        validationStatus.setValid(true);
        return validationStatus;
    }

    /**
     * Searches the outer domain data for the eid value.
     * 
     * @param image Image message to search through.
     * @return The eid value if found. {@code null} if not found.
     */
    private Object findEidValue(ImageMessage image) {
        return image.getOuterDomainData().getDomainId();
    }

    /**
     * Builds a validation status set to invalid when the eid is not found.
     * 
     * @param image Image message holding data to build the validation status. 
     * @return A ValidationStatus that is set to invalid.
     */
    private ValidationStatus buildFailedValidationStatus(ImageMessage image) {
        String errorMsg = "expediaId is required when imageType is {0}.";
        final ValidationStatus validationStatus = new ValidationStatus();
        validationStatus.setValid(false);
        errorMsg = MessageFormat.format(errorMsg, image.getOuterDomainData().getDomain());
        validationStatus.setMessage(errorMsg);
        LOGGER.debug("ErrorMessage={}", Arrays.asList(errorMsg), image);
        return validationStatus;
    }
}
