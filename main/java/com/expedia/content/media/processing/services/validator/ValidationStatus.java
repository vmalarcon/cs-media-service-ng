package com.expedia.content.media.processing.services.validator;

/**
 * This class contain validation result, true means validation successful.
 * false means validation fail, for validation fail, the message will be set to specify with property failure.
 */
public class ValidationStatus {

    private boolean status;
    private String message;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
