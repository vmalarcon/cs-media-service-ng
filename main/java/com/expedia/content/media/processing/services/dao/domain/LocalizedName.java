package com.expedia.content.media.processing.services.dao.domain;

/**
 * Represents the Localized Name of a Category
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
public class LocalizedName {
    private final String localizedName;
    private final String localeId;

    public LocalizedName(String localizedName, String localeId) {
        this.localizedName = localizedName;
        this.localeId = localeId;
    }

    public String getLocalizedName() {
        return localizedName;
    }

    public String getLocaleId() {
        return localeId;
    }
}
