package com.expedia.content.media.processing.services.validator;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * This class contain validation result, true means validation successful.
 * false means validation fail, for validation fail, the message will be set to specify with property failure.
 */
@SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName"})
@AllArgsConstructor
@NoArgsConstructor
public class ValidationStatus {

    private boolean isValid;
    private String message;
    private HttpStatus status;

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

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }
}
