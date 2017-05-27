package com.expedia.content.media.processing.services.dao.domain;

import lombok.Getter;

@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField", "PMD.ImmutableField"})
public class DomainCategory {
    @Getter private String parentCategoryId;
    @Getter private String categoryId;
    @Getter private String localeId;
    @Getter private String localizedName;

    public DomainCategory(String parentCategoryId, String categoryId, String localeId, String localizedName) {
        this.parentCategoryId = parentCategoryId;
        this.categoryId = categoryId;
        this.localeId = localeId;
        this.localizedName = localizedName;
    }
}
