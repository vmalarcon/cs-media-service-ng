package com.expedia.content.media.processing.services.dao.domain;

/**
 * Represents the MediaCategory from the MediaCategoryLoc Table
 */
public class MediaCategory {
    private final String mediaCategoryID;
    private final String langID;
    private final String mediaCategoryName;

    public MediaCategory(String mediaCategoryID, String langID, String mediaCategoryName) {
        this.mediaCategoryID = mediaCategoryID;
        this.langID = langID;
        this.mediaCategoryName = mediaCategoryName;
    }

    public String getMediaCategoryID() {
        return mediaCategoryID;
    }

    public String getLangID() {
        return langID;
    }

    public String getMediaCategoryName() {
        return mediaCategoryName;
    }
}
