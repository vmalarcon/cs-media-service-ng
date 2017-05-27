package com.expedia.content.media.processing.services.dao.domain;

/**
 * Represents the data retrieved from the media process log.
 */
@SuppressWarnings({"PMD.ImmutableField"})
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

    public String getMediaFileName() {
        return mediaFileName;
    }

    public String getActivityType() {
        return activityType;
    }

    public String getMediaType() {
        return mediaType;
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
