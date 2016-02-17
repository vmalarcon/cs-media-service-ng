package com.expedia.content.media.processing.services.dao;


import java.util.Date;
import java.util.List;

public class MediaSubCategoryCache {

    private final List<String> mediaSubCategoryIds;
    private final Date expiryDate;

    public MediaSubCategoryCache(List<String> mediaSubCategoryIds, Date expiryDate) {
        this.mediaSubCategoryIds = mediaSubCategoryIds;
        this.expiryDate = expiryDate;
    }

    public List<String> getMediaSubCategoryCache() {
        return mediaSubCategoryIds;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }
}
