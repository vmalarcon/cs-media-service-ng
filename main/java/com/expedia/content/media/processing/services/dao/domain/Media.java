package com.expedia.content.media.processing.services.dao.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class encapsulated the media properties before store in the database The
 * Media is build base on the received image message
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "cs-mediadb-Media")
@SuppressWarnings({"PMD.TooManyFields", "PMD.ExcessivePublicCount"})
public class Media {

    @Setter private String mediaGuid;
    @Setter private String fileUrl;
    @Setter private String fileName;
    @Setter private Long fileSize;
    @Setter private Integer width;
    @Setter private Integer height;
    @Setter private String sourceUrl;
    @Setter private String callback;
    @Setter private String domain;
    @Setter private String domainId;
    @Setter private String domainFields;
    @Setter private Date lastUpdated;
    @Setter private String active;
    @Setter private String provider;
    @Setter private String clientId;
    @Setter private String userId;
    @Setter private String metadata;
    @Setter private String derivatives;
    private String pHash;
    @Setter private String sha1;
    @Setter private String environment;
    @Setter private String lcmMediaId;
    @Setter private List<Map<String, Object>> derivativesList;
    @Setter private Map<String, Object> domainData;
    @Setter private List<String> commentList;
    @Setter private String status;
    
    @DynamoDBHashKey
    @DynamoDBAttribute(attributeName = "MediaGuid")
    public String getMediaGuid() {
        return mediaGuid;
    }

    @DynamoDBAttribute(attributeName = "MediaFileUrl")
    public String getFileUrl() {
        return fileUrl;
    }

    @DynamoDBAttribute(attributeName = "MediaFileName")
    public String getFileName() {
        return fileName;
    }

    @DynamoDBIgnore
    public Long getFileSize() {
        return fileSize;
    }

    @DynamoDBIgnore
    public Integer getWidth() {
        return width;
    }

    @DynamoDBIgnore
    public Integer getHeight() {
        return height;
    }

    @DynamoDBAttribute(attributeName = "sourceUrl")
    public String getSourceUrl() {
        return sourceUrl;
    }

    @DynamoDBAttribute(attributeName = "MediaCallback")
    public String getCallback() {
        return callback;
    }

    @DynamoDBAttribute(attributeName = "Domain")
    public String getDomain() {
        return domain;
    }

    @DynamoDBAttribute(attributeName = "DomainID")
    public String getDomainId() {
        return domainId;
    }

    @DynamoDBAttribute(attributeName = "DomainField")
    public String getDomainFields() {
        return domainFields;
    }

    @DynamoDBAttribute(attributeName = "LastUpdated")
    public Date getLastUpdated() {
        return lastUpdated;
    }

    @DynamoDBAttribute(attributeName = "Active")
    public String getActive() {
        return active;
    }

    @DynamoDBAttribute(attributeName = "Provider")
    public String getProvider() {
        return provider;
    }

    @DynamoDBAttribute(attributeName = "ClientId")
    public String getClientId() {
        return clientId;
    }

    @DynamoDBAttribute(attributeName = "UserId")
    public String getUserId() {
        return userId;
    }

    @DynamoDBAttribute(attributeName = "MetaDatas")
    public String getMetadata() {
        return metadata;
    }

    @DynamoDBAttribute(attributeName = "Derivatives")
    public String getDerivatives() {
        return derivatives;
    }

    @DynamoDBAttribute(attributeName = "PHASH")
    public String getpHash() {
        return pHash;
    }
    
    public void setpHash(String pHash) {
        this.pHash = pHash;
    }

    @DynamoDBAttribute(attributeName = "SHA-1")
    public String getSha1() {
        return sha1;
    }

    @DynamoDBAttribute(attributeName = "Environment")
    public String getEnvironment() {
        return environment;
    }

    @DynamoDBAttribute(attributeName = "LcmMediaId")
    public String getLcmMediaId() {
        return lcmMediaId;
    }

    @DynamoDBIgnore
    public List<Map<String, Object>> getDerivativesList() {
        return derivativesList;
    }

    @DynamoDBIgnore
    public Map<String, Object> getDomainData() {
        return domainData;
    }

    @DynamoDBIgnore
    public List<String> getCommentList() {
        return commentList;
    }

    @DynamoDBIgnore
    public String getStatus() {
        return status == null ? "RECEIVED" :status;
    }

}
