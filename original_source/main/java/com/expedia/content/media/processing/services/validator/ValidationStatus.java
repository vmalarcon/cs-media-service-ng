package com.expedia.content.media.processing.services.validator;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * This class contain validation result, true means validation successful.
 * false means validation fail, for validation fail, the message will be set to specify with property failure.
 */
@SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName"})
@AllArgsConstructor
@NoArgsConstructor
public class ValidationStatus {
    public static final String ZERO_BYTES = "0 Bytes";
    public static final String NOT_FOUND = "not found";
    public static final String INVALID = "invalid";
    public static final String VALID = "valid";

    private boolean isValid;
    private String message;
    private String status;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
