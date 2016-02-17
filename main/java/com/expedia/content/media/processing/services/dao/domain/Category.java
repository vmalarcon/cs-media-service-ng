package com.expedia.content.media.processing.services.dao.domain;

import java.util.List;

/**
 * Represents a Domain Category
 */
public class Category {
    private final String categoryId;
    private final List<LocalizedName> categoryName;
    private final List<Subcategory> subcategories;

    public Category(String categoryId, List<LocalizedName> categoryName, List<Subcategory> subcategories) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.subcategories = subcategories;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public List<LocalizedName> getCategoryName() {
        return categoryName;
    }

    public List<Subcategory> getSubcategories() {
        return subcategories;
    }
}
