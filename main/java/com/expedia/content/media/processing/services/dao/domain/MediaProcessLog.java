package com.expedia.content.media.processing.services.dao.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.NoArgsConstructor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents the data retrieved from the media process log.
 */
@NoArgsConstructor
@DynamoDBTable(tableName = "cs-mediadb-MediaProcessLog")
public class MediaProcessLog {

    private String activityTime;
    private String mediaFileName;
    private String activityType;
    private String mediaType;
    private String mediaGuid;
    private String domainId;

    public MediaProcessLog(String activityTime, String mediaFileName, String activityNameAndType, String mediaType) {
        this.activityTime = activityTime;
        this.mediaFileName = mediaFileName;
        this.activityType = activityNameAndType;
        this.mediaType = mediaType;
    }

    @DynamoDBHashKey
    @DynamoDBAttribute(attributeName = "MediaGUID")
    public String getMediaGuid() {
        return mediaGuid;
    }

    public void setMediaGuid(String mediaGuid) {
        this.mediaGuid = mediaGuid;
    }

    @DynamoDBAttribute(attributeName = "activityTime")
    public String getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(String activityTime) {
        this.activityTime = activityTime;
    }

    @DynamoDBAttribute(attributeName = "FileName")
    public String getMediaFileName() {
        return mediaFileName;
    }

    public void setMediaFileName(String mediaFileName) {
        this.mediaFileName = mediaFileName;
    }

    @DynamoDBAttribute(attributeName = "ActivityType")
    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    @DynamoDBAttribute(attributeName = "MediaType")
    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    @DynamoDBAttribute(attributeName = "DomainId")
    @DynamoDBIndexHashKey(globalSecondaryIndexName = "cs-mediadb-index-MediaProcessLog-DomainId")
    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public Date getStatusDate() {
        try {
            final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
            return dateFormat.parse(activityTime);
        } catch (ParseException e) {
            return new Date(0L);
        }
    }

    @Override public String toString() {
        return "MediaProcessLog{" +
                "activityTime='" + activityTime + '\'' +
                ", mediaFileName='" + mediaFileName + '\'' +
                ", activityType='" + activityType + '\'' +
                ", mediaType='" + mediaType + '\'' +
                ", mediaGuid='" + mediaGuid + '\'' +
                '}';
    }
}
