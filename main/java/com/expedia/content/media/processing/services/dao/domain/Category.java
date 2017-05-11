package com.expedia.content.media.processing.services.dao.domain;

import java.util.List;

/**
 * Represents a Domain Category
 */
public class Category {
    private String categoryId;
    private List<LocalizedName> categoryName;
    private List<Subcategory> subcategories;

    public Category(String categoryId, List<LocalizedName> categoryName, List<Subcategory> subcategories) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.subcategories = subcategories;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public List<LocalizedName> getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(List<LocalizedName> categoryName) {
        this.categoryName = categoryName;
    }

    public List<Subcategory> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<Subcategory> subcategories) {
        this.subcategories = subcategories;
    }
}
