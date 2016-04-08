package com.expedia.content.media.processing.services.dao.domain;

import lombok.Builder;
import lombok.Getter;
import java.util.Date;

/**
 * Represents the media data from the the Media and CatalogItemMedia tables.
 */
@Builder
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.ImmutableField","PMD.TooManyFields"})
public class LcmCatalogItemMedia {

    @Getter private final Integer catalogItemId;
    @Getter private final Integer mediaId;
    @Getter private final Integer mediaUseRank;
    @Getter private final String fileName;
    @Getter private final String lastUpdatedBy;
    @Getter private final Date lastUpdateDate;
    @Getter private final String comment;

}
