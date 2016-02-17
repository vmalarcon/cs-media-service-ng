package com.expedia.content.media.processing.services.dao.domain;

import java.util.List;

/**
 * Represents the Sub Category of a Category
 */
public class Subcategory {
    private final String subcategoryId;
    private final List<LocalizedName> subcategoryName;

    public Subcategory(String subcategoryId, List<LocalizedName> subcategoryName) {
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
