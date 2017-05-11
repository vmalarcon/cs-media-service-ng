package com.expedia.content.media.processing.services.dao.domain;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.InvalidDomainException;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * This class encapsulated the media properties before store in the database The
 * Media is build base on the received image message
 */

@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "cs-mediadb-Media")
@SuppressWarnings({"PMD.TooManyFields", "PMD.ExcessivePublicCount","PMD.UnusedPrivateField", "PMD.SingularField", "PMD.ImmutableField"})
public class Media {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Setter private String mediaGuid;
    @Setter private String fileUrl;
    @Setter private String fileName;
    @Setter private Long fileSize;
    @Setter private Integer width;
    @Setter private Integer height;
    @Setter private String sourceUrl;
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
    @Setter private String domainDerivativeCategory;
    @Setter private Boolean propertyHero;
    @Setter private Boolean hidden;
    @Setter private String providedName;

    public static Media of(Media media) {
        return new Media(media.mediaGuid, media.fileUrl, media.fileName, media.fileSize, media.width, media.height, media.sourceUrl, media.domain, media.domainId, media.domainFields,
                media.lastUpdated, media.active, media.provider, media.clientId, media.userId, media.metadata, media.derivatives, media.pHash, media.sha1, media.environment,
                media.lcmMediaId, media.derivativesList, media.domainData, media.commentList, media.status, media.domainDerivativeCategory, media.propertyHero, media.hidden,
                media.providedName);
    }

    @DynamoDBHashKey
    @DynamoDBAttribute(attributeName = "MediaGUID")
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

    @DynamoDBAttribute(attributeName = "SourceUrl")
    public String getSourceUrl() {
        return sourceUrl;
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

    @DynamoDBAttribute(attributeName = "lcmMediaId")
    public String getLcmMediaId() {
        return lcmMediaId;
    }

    public Boolean getPropertyHero() {
        final Map domainMap = JSONUtil.buildMapFromJson(domainFields);
        if (domainMap == null) {
            return false;
        }
        final String hero = (String) domainMap.get("propertyHero");
        return "true".equalsIgnoreCase(hero);
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
        return status == null ? "RECEIVED" : status;
    }

    @DynamoDBAttribute(attributeName = "DerivativeCategory")
    public String getDomainDerivativeCategory() {
        return domainDerivativeCategory;
    }

    @DynamoDBAttribute(attributeName = "hidden")
    public Boolean isHidden(){
        return hidden == null ? false : hidden;
    }

    @DynamoDBAttribute(attributeName = "ProvidedName")
    public String getProvidedName() {
        return providedName;
    }


    // START OF TEMPORARY METHODS -- will remove once DynamoDB is no longer being used.

    /**
     * Converts the Media to an ImageMessage.
     * @return returns ImageMessage of Media.
     */
    public ImageMessage toImageMessage() {
        final Map<String, Object> outerDomainMap = new HashMap<>();
        outerDomainMap.put("domain", this.getDomain());
        outerDomainMap.put("domainId", this.getDomainId());
        outerDomainMap.put("domainDerivativeCategory", this.getDomainDerivativeCategory());
        outerDomainMap.put("domainProvider", this.getProvider());
        if(!StringUtils.isEmpty(this.getDomainFields())){
            outerDomainMap.put("domainFields", (Map<String, Object>) JSONUtil.buildMapFromJson(this.getDomainFields()));
        }
        final OuterDomain outerDomain = retrieveOuterDomainDomain(outerDomainMap);

        return ImageMessage.builder()
                .clientId(this.getClientId())
                .userId(this.getUserId())
                // no requestId
                .mediaGuid(this.getMediaGuid())
                .fileUrl(this.getFileUrl())
                .fileName(this.getFileName())
                .active(this.getActive() == null || Boolean.parseBoolean(this.getActive()))
                // no callback
                .hidden(this.isHidden())
                .providedName(this.getProvidedName())
                // no categoryId
                // no caption
                // no providerId
                .comment((this.getCommentList() == null || this.getCommentList().isEmpty()) ? "" : this.getCommentList().get(0))
                .outerDomainData(outerDomain)
                // no staging-key
                // no logEntries
                .build();
    }

    private static OuterDomain retrieveOuterDomainDomain(Map<String, Object> mapMessage) {
        final String domainName = (String)mapMessage.get("domain");
        if(domainName == null) {
            return null;
        } else {
            Domain domain = null;

            try {
                domain = getDomain(domainName);
            } catch (InvalidDomainException var5) {
                final String errorMsg = MessageFormat.format("{0}: {1} is not a recognized domain.", new Object[]{"domain", domainName});
                throw new ImageMessageException(errorMsg, var5);
            }

            final Map domainField = buildDomainField(mapMessage);
            return new OuterDomain(domain, (String)mapMessage.get("domainId"), (String)mapMessage.get("domainProvider"), (String)mapMessage.get("domainDerivativeCategory"), domainField);
        }
    }

    private static Domain getDomain(String domainName) throws InvalidDomainException {
        final Domain imageTypeMatch = Domain.findDomain(domainName);
        if(imageTypeMatch == null) {
            throw new InvalidDomainException("ERROR - Domain " + domainName + " not recognized.");
        } else {
            return imageTypeMatch;
        }
    }

    private static Map<String, Object> buildDomainField(Map<String, Object> mapMessage) {
        Map domainField = null;
        if(mapMessage.get("domainFields") instanceof Map) {
            domainField = (Map)mapMessage.get("domainFields");
        } else if(mapMessage.get("domainFields") instanceof String) {
            domainField = buildMapForDomainField((String)mapMessage.get("domainFields"));
        }

        return domainField;
    }

    private static Map buildMapForDomainField(String jsonMessage) {
        try {
            return (Map)OBJECT_MAPPER.readValue(jsonMessage, Map.class);
        } catch (IOException var1) {
            MessageFormat.format("Error parsing/converting Json message: {0}", new Object[]{jsonMessage});
            return null;
        }
    }
}
