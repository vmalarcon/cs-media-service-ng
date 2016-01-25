package com.expedia.content.media.processing.services.dao;

/**
 * Represents the MediaCategory from the MediaCategoryLoc Table
 */
public class MediaCategory {
    private String mediaCategoryID;
    private String langID;
    private String mediaCategoryName;

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
