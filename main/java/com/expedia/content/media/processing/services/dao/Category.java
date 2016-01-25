package com.expedia.content.media.processing.services.dao;

import java.util.List;

/**
 * Represents a Domain Category
 */
public class Category {
    private String categoryId;
    private List<LocalizedName> categoryName;
    private List<SubCategory> subcategories;

    public Category(String categoryId, List<LocalizedName> categoryName, List<SubCategory> subcategories) {
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

    public List<SubCategory> getSubcategories() {
        return subcategories;
    }
}
