package com.expedia.content.media.processing.services.dao.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * An Object to represent a domain category record in MediaDB.
 */
@Getter
@Builder
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField", "PMD.ImmutableField"})
public class DomainCategory {
    private String parentCategoryId;
    private String categoryId;
    private String localeId;
    private String localizedName;
}
