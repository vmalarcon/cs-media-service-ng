package com.expedia.content.media.processing.services.dao.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import lombok.Builder;

/**
 * Encapsulated a MediaDerivative before store in the database
 */
@Builder
@DynamoDBTable(tableName = "cs-mediadb-MediaDerivative")
public class MediaDerivative {
    
    private final String mediaGuid;
    private final String type;
    private final String width;
    private final String height;
    private final String fileSize;
    private final String location;
    
    @DynamoDBHashKey
    @DynamoDBAttribute(attributeName = "MediaGUID")
    public String getMediaGuid() {
        return mediaGuid;
    }
    
    @DynamoDBRangeKey
    @DynamoDBAttribute(attributeName = "DerivativeType")
    public String getType() {
        return type;
    }
    
    @DynamoDBAttribute(attributeName = "Width")
    public String getWidth() {
        return width;
    }
    
    @DynamoDBAttribute(attributeName = "Height")
    public String getHeight() {
        return height;
    }
    
    @DynamoDBAttribute(attributeName = "FileSize")
    public String getFileSize() {
        return fileSize;
    }
    
    @DynamoDBAttribute(attributeName = "Location")
    public String getLocation() {
        return location;
    }
}
