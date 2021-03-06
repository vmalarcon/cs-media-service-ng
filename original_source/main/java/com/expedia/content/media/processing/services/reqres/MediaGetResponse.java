package com.expedia.content.media.processing.services.reqres;

import java.util.List;
import java.util.Map;

import com.expedia.content.media.processing.services.dao.domain.Comment;
import lombok.Builder;
import lombok.Getter;

/**
 * Media Get response object.
 */
@Builder
@Getter
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class MediaGetResponse {
    
    @SuppressWarnings("CPD-START")
    private final String mediaGuid;
    private final String fileUrl;
    private final String sourceUrl;
    private final String fileName;
    private final String active;
    private final Integer width;
    private final Integer height;
    private final Long fileSize;
    private final String status;
    private final String lastUpdatedBy;
    private final String lastUpdateDateTime;
    private final String domainProvider;
    private final String domainDerivativeCategory;
    private final Map<String, Object> domainFields;
    private final List<Map<String, Object>> derivatives;
    private final List<Comment> comments;
    @SuppressWarnings("CPD-END")
    private final String domain;
    private final String domainId;

}
