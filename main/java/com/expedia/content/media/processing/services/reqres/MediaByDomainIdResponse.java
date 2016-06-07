package com.expedia.content.media.processing.services.reqres;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Media By domain id message response.
 */
@Builder
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class MediaByDomainIdResponse {

    @Getter private final String domain;
    @Getter private final String domainId;
    @Getter private final Integer totalMediaCount;
    @Getter private final List<DomainIdMedia> images;

}
