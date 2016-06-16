package com.expedia.content.media.processing.services.dao.domain;

/**
 * Represents the data retrieved from the media process log.
 */
public class MediaProcessLog {

    private String activityTime;
    private String mediaFileName;
    private String activityType;
    private String mediaType;

    public MediaProcessLog(String activityTime, String mediaFileName, String activityNameAndType, String mediaType) {
        this.activityTime = activityTime;
        this.mediaFileName = mediaFileName;
        this.activityType = activityNameAndType;
        this.mediaType = mediaType;
    }

    public String getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(String activityTime) {
        this.activityTime = activityTime;
    }

    public String getMediaFileName() {
        return mediaFileName;
    }

    public void setMediaFileName(String mediaFileName) {
        this.mediaFileName = mediaFileName;
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

    @Override public String toString() {
        return "MediaProcessLog{" +
                "activityTime='" + activityTime + '\'' +
                ", mediaFileName='" + mediaFileName + '\'' +
                ", activityType='" + activityType + '\'' +
                ", mediaType='" + mediaType + '\'' +
                '}';
    }
}
