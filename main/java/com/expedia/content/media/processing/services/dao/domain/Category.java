package com.expedia.content.media.processing.services.dao.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Represents a Domain Category
 */
@AllArgsConstructor
@Getter
public class Category {
    private String categoryId;
    private List<LocalizedName> categoryName;
    private List<Subcategory> subcategories;
}
