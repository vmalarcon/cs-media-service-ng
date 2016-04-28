package com.expedia.content.media.processing.services.dao.domain;

import java.util.Date;

import lombok.Builder;
import lombok.Getter;

/**
 * Represents the media data from the the Media and CatalogItemMedia tables.
 */
@Builder
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.ImmutableField","PMD.TooManyFields"})
public class LcmMediaAndDerivative {

    @Getter private final Integer domainId;
    @Getter private final Integer mediaId;
    @Getter private final String fileName;
    @Getter private final Boolean active;
    @Getter private final Integer width;
    @Getter private final Integer height;
    @Getter private final Integer fileSize;
    @Getter private final String lastUpdatedBy;
    @Getter private final Date lastUpdateDate;
    @Getter private final String mediaLastUpdatedBy;
    @Getter private final Date mediaLastUpdateDate;
    @Getter private final Integer provider;
    @Getter private final Integer category;
    @Getter private final String comment;
    @Getter private final Integer formatId;
    @Getter private final Boolean filProcessedBool;
    @Getter private final String mediaCreditTxt;
    @Getter private final Double mediaStartHorizontalPct;
    @Getter private final Short mediaDisplayMethodSeqNbr;
    @Getter private final String mediaCaptionTxt;
    @Getter private final String mediaDisplayName;
    @Getter private final String derivativeFileName;
    @Getter private final Integer derivativeSizeTypeId;
    @Getter private final Boolean fileProcessed;
    @Getter private final Integer derivativeWidth;
    @Getter private final Integer derivativeHeight;
    @Getter private final Integer derivativeFileSize;

}
