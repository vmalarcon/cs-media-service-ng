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
import lombok.Getter;
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
@Getter
@Setter
@SuppressWarnings({"PMD.TooManyFields", "PMD.ExcessivePublicCount","PMD.UnusedPrivateField", "PMD.SingularField", "PMD.ImmutableField"})
public class Media {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String mediaGuid;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String sourceUrl;
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
    private List<Map<String, Object>> derivativesList;
    private Map<String, Object> domainData;
    private List<String> commentList;
    private String status;
    private String domainDerivativeCategory;
    private Boolean propertyHero;
    private Boolean hidden;
    private String providedName;

    public static Media of(Media media) {
        return new Media(media.mediaGuid, media.fileUrl, media.fileName, media.fileSize, media.width, media.height, media.sourceUrl, media.domain, media.domainId, media.domainFields,
                media.lastUpdated, media.active, media.provider, media.clientId, media.userId, media.metadata, media.derivatives, media.pHash, media.sha1, media.environment,
                media.lcmMediaId, media.derivativesList, media.domainData, media.commentList, media.status, media.domainDerivativeCategory, media.propertyHero, media.hidden,
                media.providedName);
    }

    public Boolean getPropertyHero() {
        final Map domainMap = JSONUtil.buildMapFromJson(domainFields);
        if (domainMap == null) {
            return false;
        }
        final String hero = (String) domainMap.get("propertyHero");
        return "true".equalsIgnoreCase(hero);
    }

    public String getStatus() {
        return status == null ? "RECEIVED" : status;
    }

    public Boolean isHidden(){
        return hidden == null ? false : hidden;
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
        final String domainName = (String) mapMessage.get("domain");
        if(domainName == null) {
            return null;
        } else {
            Domain domain;
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
            return OBJECT_MAPPER.readValue(jsonMessage, Map.class);
        } catch (IOException var1) {
            MessageFormat.format("Error parsing/converting Json message: {0}", new Object[]{jsonMessage});
            return null;
        }
    }
}
