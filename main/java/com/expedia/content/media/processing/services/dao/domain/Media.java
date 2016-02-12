package com.expedia.content.media.processing.services.dao.domain;

import java.util.Date;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * This class encapsulated the media properties before store in the database The
 * Media is build base on the received image message
 */

@DynamoDBTable(tableName = "cs-mediadb-Media")
@SuppressWarnings("PMD.TooManyFields")
public class Media {

    private String mediaGuid;
    private String fileUrl;
    private String fileName;
    private String sourceUrl;
    private String callback;
    private String domain;
    private String domainId;
    private String domainFields;
    private Date lastUpdated;
    private String active;
    private String provider;
    private String clientId;
    private String userId;
    private String metadata;
    private String derivatives;
    private String pHash;
    private String sha1;
    private String environment;
    private String lcmMediaId;
    private String status = "RECEIVED";

    @DynamoDBHashKey
    @DynamoDBAttribute(attributeName = "MediaGuid")
    public String getMediaGuid() {
        return mediaGuid;
    }

    public void setMediaGuid(String mediaGuid) {
        this.mediaGuid = mediaGuid;
    }

    @DynamoDBAttribute(attributeName = "MediaFileUrl")
    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    @DynamoDBAttribute(attributeName = "MediaFileName")
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @DynamoDBAttribute(attributeName = "sourceUrl")
    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    @DynamoDBAttribute(attributeName = "MediaCallback")
    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
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

    @DynamoDBAttribute(attributeName = "DomainField")
    public String getDomainFields() {
        return domainFields;
    }

    public void setDomainFields(String domainFields) {
        this.domainFields = domainFields;
    }

    @DynamoDBAttribute(attributeName = "LastUpdated")
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @DynamoDBAttribute(attributeName = "Active")
    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    @DynamoDBAttribute(attributeName = "Provider")
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @DynamoDBAttribute(attributeName = "ClientId")
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @DynamoDBAttribute(attributeName = "UserId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDBAttribute(attributeName = "MetaDatas")
    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    @DynamoDBAttribute(attributeName = "Derivatives")
    public String getDerivatives() {
        return derivatives;
    }

    public void setDerivatives(String derivatives) {
        this.derivatives = derivatives;
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

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @DynamoDBAttribute(attributeName = "Environment")
    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @DynamoDBAttribute(attributeName = "LcmMediaId")
    public String getLcmMediaId() {
        return lcmMediaId;
    }

    public void setLcmMediaId(String lcmMediaId) {
        this.lcmMediaId = lcmMediaId;
    }

    @DynamoDBIgnore
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
