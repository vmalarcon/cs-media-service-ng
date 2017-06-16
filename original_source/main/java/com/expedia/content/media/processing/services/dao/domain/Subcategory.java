package com.expedia.content.media.processing.services.dao.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Represents the Sub Category of a Category
 */
@AllArgsConstructor
@Getter
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class Subcategory {
    private final String subcategoryId;
    private final List<LocalizedName> subcategoryName;
}
