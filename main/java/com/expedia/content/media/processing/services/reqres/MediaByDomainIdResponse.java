package com.expedia.content.media.processing.services.reqres;

import java.util.List;

import com.expedia.content.media.processing.services.dao.domain.DomainIdMedia;
import lombok.Builder;
import lombok.Getter;

/**
 * Media By domain id message response.
 */
@Builder
@Getter
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class MediaByDomainIdResponse {
    private final String domain;
    private final String domainId;
    private final Integer totalMediaCount;
    private final List<DomainIdMedia> images;

}
