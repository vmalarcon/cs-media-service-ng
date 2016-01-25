package com.expedia.content.media.processing.services.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain Categories DAO.
 */
@Component
public class MediaDomainCategoriesDao {
    private static final int SKIP_NULL_AND_DELETED_CATEGORIES = 2;
    private SQLMediaDomainCategoriesSproc sproc;

    @Autowired
    public MediaDomainCategoriesDao(SQLMediaDomainCategoriesSproc sproc) {
        this.sproc = sproc;
    }

    /**
     * Using the Result Sets of SQLMediaDomainCategoriesSproc this Method groups the results by Category ID and then
     * populates each Category with Localized Names and SubCategories
     * @see Category, LocalizedName, SubCategory, SQLMediaDomainCategoriesSproc
     *
     * @param domain        The Domain to query
     * @param localeId      The Localization Id to query by
     * @return List of Category Objects
     * @throws Exception
     */
    public List<Category> getMediaCategoriesWithSubCategories(String domain, String localeId) throws Exception {
        if (!"lodging".equals(domain)) {
            throw new DomainNotFoundException("Domain Not Found");
        }
        Map<String, Object> results = sproc.execute(localeId);
        List<MediaCategory> mediaCategories = (List<MediaCategory>) results.get(SQLMediaDomainCategoriesSproc.MEDIA_CATEGORY_RESULT_SET);
        List<MediaSubCategory> mediaSubCategories = (List<MediaSubCategory>) results.get(SQLMediaDomainCategoriesSproc.MEDIA_SUB_CATEGORY_RESULT_SET);

        Map<Integer, List<MediaCategory>> categoryMap = mediaCategories.stream()
                .filter(category -> Integer.parseInt(category.getMediaCategoryID()) > SKIP_NULL_AND_DELETED_CATEGORIES)
                .collect(Collectors.groupingBy(category -> Integer.parseInt(category.getMediaCategoryID())));
        Map<Integer, List<MediaSubCategory>> subCategoryMap = mediaSubCategories.stream()
                .collect(Collectors.groupingBy(subCategory -> Integer.parseInt(subCategory.getMediaCategoryID())));
        List<Category> categoriesList = categoryMap.keySet().stream()
                .map(categoryId -> new Category(String.valueOf(categoryId), categoryMap.get(categoryId).stream()
                        .map(item -> new LocalizedName(item.getMediaCategoryName(), item.getLangID()))
                        .collect(Collectors.toList()), getSubCategoryList(categoryId, subCategoryMap)))
                .collect(Collectors.toList());
        return categoriesList;
    }

    /**
     * Builds a List of SubCategory Objects by CategoryId
     * @see SubCategory
     *
     * @param categoryId        The CategoryID of which the SubCategories belong
     * @param subCategoryMap    A HashMap of subCategories grouped by CategoryId
     * @return List of SubCategory Objects
     */
    private List<SubCategory> getSubCategoryList(Integer categoryId, Map<Integer, List<MediaSubCategory>> subCategoryMap) {
        Map<Integer, List<MediaSubCategory>> innerSubCategoryMap = subCategoryMap.get(categoryId).stream()
                .collect(Collectors.groupingBy(subCategory -> Integer.parseInt(subCategory.getMediaSubCategoryID())));
        List<SubCategory> subCategoriesList = innerSubCategoryMap.keySet().stream()
                .map(subCategoryId -> new SubCategory(String.valueOf(subCategoryId), innerSubCategoryMap.get(subCategoryId).stream()
                        .map(item -> new LocalizedName(item.getMediaSubCategoryName(), item.getLangID()))
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
        return subCategoriesList;
    }
}
