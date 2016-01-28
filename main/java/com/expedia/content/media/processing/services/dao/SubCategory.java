package com.expedia.content.media.processing.services.dao;

import java.util.List;

/**
 * Represents the Sub Category of a Category
 */
public class SubCategory {
    private final String subCategoryId;
    private final List<LocalizedName> subCategoryName;

    public SubCategory(String subCategoryId, List<LocalizedName> subCategoryName) {
        this.subCategoryId = subCategoryId;
        this.subCategoryName = subCategoryName;
    }

    public String getSubCategoryId() {
        return subCategoryId;
    }

    public List<LocalizedName> getSubCategoryName() {
        return subCategoryName;
    }
}
