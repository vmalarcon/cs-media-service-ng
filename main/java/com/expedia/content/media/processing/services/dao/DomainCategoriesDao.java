package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.services.dao.domain.Category;
import com.expedia.content.media.processing.services.exception.DomainNotFoundException;

import java.util.List;

public interface DomainCategoriesDao {

    List<Category> getMediaCategoriesWithSubCategories(String domain, String localeId) throws DomainNotFoundException;
}
