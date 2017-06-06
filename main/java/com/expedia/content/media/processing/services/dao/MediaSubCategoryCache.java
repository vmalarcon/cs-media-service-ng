package com.expedia.content.media.processing.services.dao;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;
import java.util.List;

/**
 * Cache object to store mediaSubcategories in memory.
 */
@AllArgsConstructor
@Getter
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class MediaSubCategoryCache {
    private final List<String> mediaSubCategoryIds;
    private final Date expiryDate;

    public List<String> getMediaSubCategoryCache() {
        return mediaSubCategoryIds;
    }
}
