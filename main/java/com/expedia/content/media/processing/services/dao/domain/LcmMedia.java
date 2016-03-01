package com.expedia.content.media.processing.services.dao.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Represents the media data from the the Media and CatalogItemMedia tables.
 */
@Builder
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.ImmutableField"})
public class LcmMedia {

    @Getter private final Integer domainId;
    @Getter private final Integer mediaId;
    @Getter private final String fileName;
    @Getter private final Boolean active;
    @Getter private final Integer width;
    @Getter private final Integer height;
    @Getter private final Integer fileSize;
    @Getter private final String lastUpdatedBy;
    @Getter private final Date lastUpdateDate;
    @Getter private final Integer provider;
    @Getter private final Integer category;
    @Getter private final String comment;
    @Getter private final Integer formatId;
    @Getter private final Boolean filProcessBool;

    @Getter @Setter private List<LcmMediaDerivative> derivatives = Collections.EMPTY_LIST;
    @Getter @Setter private List<LcmMediaRoom> rooms = Collections.EMPTY_LIST;

}
