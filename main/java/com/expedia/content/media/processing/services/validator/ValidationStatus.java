package com.expedia.content.media.processing.services.validator;

/**
 * Created by seli on 2015-07-13.
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
