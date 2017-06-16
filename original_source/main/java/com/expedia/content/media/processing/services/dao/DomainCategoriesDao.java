package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.domain.Category;

import java.util.List;

/**
 * An interface for all DAOs implementations to adhere to for accessing domain categories.
 */
public interface DomainCategoriesDao {

    /**
     * Returns all the categories and subcategories for a specific domain.
     * note: if localeId is null, the categories and subcategories are returned for all languages.
     *
     * @param domain The domain to query categories and subcategories for.
     * @param localeId The localeId of the categories and subcategories to be returned.
     * @return A list of Category objects.
     */
    List<Category> getMediaCategoriesWithSubCategories(String domain, String localeId);

    /**
     * verifies if the subcategoryId in the OuterDomain exists in the Database.
     *
     * @param outerDomain Domain specific data.
     * @param localeId The localeId of the subcategoryId.
     * @return True is subcategory exists, otherwise false.
     */
    Boolean subCategoryIdExists(OuterDomain outerDomain, String localeId);
}
