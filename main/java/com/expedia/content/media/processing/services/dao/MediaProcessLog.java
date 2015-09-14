package com.expedia.content.media.processing.services.dao;

/**
 * Represents the data retrieved from LCM of a MediaProcessLog.
 */
public class MediaProcessLog {

    private String activityTime;
    private String mediaFileName;
    private String activityNameAndType;

    public MediaProcessLog(String activityTime, String mediaFileName, String activityNameAndType) {
        this.activityTime = activityTime;
        this.mediaFileName = mediaFileName;
        this.activityNameAndType = activityNameAndType;
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

    public String getActivityNameAndType() {
        return activityNameAndType;
    }

    public void setActivityNameAndType(String activityNameAndType) {
        this.activityNameAndType = activityNameAndType;
    }

    public String getActivityType() {
        if (activityNameAndType != null) {
            if (activityNameAndType.indexOf("/") >= 0) {
                return activityNameAndType.substring(activityNameAndType.indexOf("/") + 1);
            } else {
                return activityNameAndType;
            }
        }
        return null;
    }

    @Override public String toString() {
        return "MediaProcessLog{" +
                "activityTime='" + activityTime + '\'' +
                ", mediaFileName='" + mediaFileName + '\'' +
                ", activityNameAndType='" + activityNameAndType + '\'' +
                '}';
    }
}
