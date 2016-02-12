package com.expedia.content.media.processing.services.dao.domain;

/**
 * Represents the MediaSubCategory from MediaSubCategoryLoc Table
 */
public class MediaSubCategory {
    private final String mediaCategoryID;
    private final String mediaSubCategoryID;
    private final String langID;
    private final String mediaSubCategoryName;

    public MediaSubCategory(String mediaCategoryID, String mediaSubCategoryID, String langID, String mediaSubCategoryName) {
        this.mediaCategoryID = mediaCategoryID;
        this.mediaSubCategoryID = mediaSubCategoryID;
        this.langID = langID;
        this.mediaSubCategoryName = mediaSubCategoryName;
    }

    public String getMediaCategoryID() {
        return mediaCategoryID;
    }

    public String getMediaSubCategoryID() {
        return mediaSubCategoryID;
    }

    public String getLangID() {
        return langID;
    }

    public String getMediaSubCategoryName() {
        return mediaSubCategoryName;
    }
}
