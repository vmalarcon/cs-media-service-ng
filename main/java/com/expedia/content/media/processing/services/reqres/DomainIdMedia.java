package com.expedia.content.media.processing.services.reqres;

import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Image object
 */
@SuppressWarnings({"PMD.UnusedPrivateField","PMD.SingularField"})
public class DomainIdMedia {

    @SuppressWarnings("CPD-START")
    @Getter private final String mediaGuid;
    @Getter private final String fileUrl;
    @Getter private final String fileName;
    @Getter private final String active;
    @Getter private final Integer width;
    @Getter private final Integer height;
    @Getter private final Long fileSize;
    @Getter private final String status;
    @Getter private final String lastUpdatedBy;
    @Getter private final String lastUpdateDateTime;
    @Getter private final String domainProvider;
    @Getter private final String domainDerivativeCategory;
    @Getter private final Map<String, Object> domainFields;
    @Getter private final List<Map<String, Object>> derivatives;
    @Getter private final List<Comment> comments;
    @SuppressWarnings("CPD-END")

    public DomainIdMedia(MediaGetResponse mediaGetResponse) {
        this.mediaGuid = mediaGetResponse.getMediaGuid();
        this.fileUrl = mediaGetResponse.getFileUrl();
        this.fileName = mediaGetResponse.getFileName();
        this.active = mediaGetResponse.getActive();
        this.width = mediaGetResponse.getWidth();
        this.height = mediaGetResponse.getHeight();
        this.fileSize = mediaGetResponse.getFileSize();
        this.status = mediaGetResponse.getStatus();
        this.lastUpdatedBy = mediaGetResponse.getLastUpdatedBy();
        this.lastUpdateDateTime = mediaGetResponse.getLastUpdateDateTime();
        this.domainProvider = mediaGetResponse.getDomainProvider();
        this.domainDerivativeCategory = mediaGetResponse.getDomainDerivativeCategory();
        this.domainFields = mediaGetResponse.getDomainFields();
        this.derivatives = mediaGetResponse.getDerivatives();
        this.comments = mediaGetResponse.getComments();
    }
}
