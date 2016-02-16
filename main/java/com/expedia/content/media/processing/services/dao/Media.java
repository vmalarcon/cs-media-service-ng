package com.expedia.content.media.processing.services.dao;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.util.Date;

/**
 * Container for Media information from DynamoDB
 */
@DynamoDBTable(tableName = "cs-mediadb-Media")
public class Media {
    private String mediaGuid;
    private String fileName;
    private String domain;
    private String domainId;
    private Date lastUpdated;
    private String active;
    private String environment;
    private String lcmMediaId;

    @DynamoDBAttribute(attributeName = "MediaGUID")
    public String getMediaGuid() {
        return mediaGuid;
    }

    public void setMediaGuid(String mediaGuid) {
        this.mediaGuid = mediaGuid;
    }

    @DynamoDBAttribute(attributeName = "MediaFileName")
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @DynamoDBAttribute(attributeName = "Domain")
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @DynamoDBAttribute(attributeName = "DomainID")
    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @DynamoDBAttribute(attributeName = "LastUpdated")
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @DynamoDBAttribute(attributeName = "Environment")
    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @DynamoDBAttribute(attributeName = "Active")
    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    @DynamoDBAttribute(attributeName = "LcmMediaId")
    public String getLcmMediaId() {
        return lcmMediaId;
    }

    public void setLcmMediaId(String lcmMediaId) {
        this.lcmMediaId = lcmMediaId;
    }

    @Override
    public String toString() {
        return "Media {" +
                "mediaGuid='" + mediaGuid + '\'' +
                ", fileName='" + fileName + '\'' +
                ", domain='" + domain + '\'' +
                ", domainId='" + domainId + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", active='" + active + '\'' +
                ", environment='" + environment + '\'' +
                ", lcmMediaId='" + lcmMediaId + '\'' +
                '}';
    }
 }
