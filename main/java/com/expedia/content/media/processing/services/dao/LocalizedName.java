package com.expedia.content.media.processing.services.dao;

/**
 * Represents the Localized Name of a Category
 */
public class LocalizedName {
    private String localizedName;
    private String localeId;

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
