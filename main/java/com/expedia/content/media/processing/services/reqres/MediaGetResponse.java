package com.expedia.content.media.processing.services.reqres;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * Media response object.
 */
@Builder
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class MediaGetResponse {
    
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
    @Getter private final String domain;
    @Getter private final String domainId;

}
