package com.expedia.content.media.processing.services.validator;

/**
 * This class contain validation result, true means validation successful.
 * false means validation fail, for validation fail, the message will be set to specify with property failure.
 */
@SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName"})
public class ValidationStatus {

    private boolean isValid;
    private String message;

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        this.isValid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
