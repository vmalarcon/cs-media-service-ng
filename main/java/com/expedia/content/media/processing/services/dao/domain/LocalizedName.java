package com.expedia.content.media.processing.services.dao.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the Localized Name of a Category
 */
@AllArgsConstructor
@Getter
@SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
public class LocalizedName {
    private final String localizedName;
    private final String localeId;
}
