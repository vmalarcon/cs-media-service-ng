package com.expedia.content.media.processing.services.dao;

import java.util.List;

/**
 * Represents the Sub Category of a Category
 */
public class SubCategory {
    private final String subcategoryId;
    private final List<LocalizedName> subcategoryName;

    public SubCategory(String subcategoryId, List<LocalizedName> subcategoryName) {
        this.subcategoryId = subcategoryId;
        this.subcategoryName = subcategoryName;
    }

    public String getSubcategoryId() {
        return subcategoryId;
    }

    public List<LocalizedName> getSubcategoryName() {
        return subcategoryName;
    }
}
