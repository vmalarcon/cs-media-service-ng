package com.expedia.content.media.processing.services.dao.domain;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.InvalidDomainException;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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

    public String getMediaGuid() {
        return mediaGuid;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getDomain() {
        return domain;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getDomainFields() {
        return domainFields;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public String getActive() {
        return active;
    }

    public String getProvider() {
        return provider;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUserId() {
        return userId;
    }

    public String getDerivatives() {
        return derivatives;
    }

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

    public List<Map<String, Object>> getDerivativesList() {
        return derivativesList;
    }

    public Map<String, Object> getDomainData() {
        return domainData;
    }

    public List<String> getCommentList() {
        return commentList;
    }

    public String getStatus() {
        return status == null ? "RECEIVED" : status;
    }

    public String getDomainDerivativeCategory() {
        return domainDerivativeCategory;
    }

    public Boolean isHidden(){
        return hidden == null ? false : hidden;
    }

    public String getProvidedName() {
        return providedName;
    }


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
