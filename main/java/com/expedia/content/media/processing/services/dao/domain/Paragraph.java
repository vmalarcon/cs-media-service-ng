package com.expedia.content.media.processing.services.dao.domain;

import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;

/**
 * Class representing Paragraph in LCM
 */
@Builder
@Getter
@SuppressWarnings("PMD.UnusedPrivateField")
public class Paragraph {
    private int catalogItemId;
    private int sectionTypeId;
    private int paragraphNbr;
    private short paragraphTypeId;
    private int langId;
    private String paragraphTxt;
    private Timestamp effectiveStartDate;
    private Timestamp effectiveEndDate;
    private int paragraphMediaId;
    private int mediaSizeTypeId;
    private Timestamp updateDate;
    private String lastUpdatedBy;
    private String lastUpdateLocation;
    private String contentSourceTypeId;
}
