package com.expedia.content.media.processing.services.dao.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the data retrieved from the media process log.
 */
@AllArgsConstructor
@Getter
@SuppressWarnings({"PMD.ImmutableField"})
public class MediaProcessLog {
    private String activityTime;
    private String mediaFileName;
    private String activityType;
    private String mediaType;

    @Override
    public String toString() {
        return "MediaProcessLog{" +
                "activityTime='" + activityTime + "', " +
                "mediaFileName='" + mediaFileName + "', " +
                "activityType='" + activityType + "', " +
                "mediaType='" + mediaType + "'}";
    }
}
