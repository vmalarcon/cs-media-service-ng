package com.expedia.content.media.processing.services.dao.domain;

import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;

/**
 * Class representing Paragraph in LCM
 */
@Builder
@SuppressWarnings("PMD.UnusedPrivateField")
public class Paragraph {
    @Getter private int catalogItemId;
    @Getter private int sectionTypeId;
    @Getter private int paragraphNbr;
    @Getter private short paragraphTypeId;
    @Getter private int langId;
    @Getter private String paragraphTxt;
    @Getter private Timestamp effectiveStartDate;
    @Getter private Timestamp effectiveEndDate;
    @Getter private int paragraphMediaId;
    @Getter private int mediaSizeTypeId;
    @Getter private Timestamp updateDate;
    @Getter private String lastUpdatedBy;
    @Getter private String lastUpdateLocation;
    @Getter private String contentSourceTypeId;
}
