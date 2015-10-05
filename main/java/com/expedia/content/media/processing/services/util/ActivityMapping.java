package com.expedia.content.media.processing.services.util;

/**
 * define static data mapping between activityType and statusMessage
 */
public class ActivityMapping {

    private String statusMessage;
    private String activityType;
    private String mediaType;

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }
}
