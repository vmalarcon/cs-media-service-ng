package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain Categories DAO.
 */
@Component
public class MediaDomainCategoriesDao {
    private static final int SKIP_NULL_AND_DELETED_CATEGORIES = 2;
    private final static String CATEGORY = "category";
    private final SQLMediaDomainCategoriesSproc sproc;

    @Autowired
    public MediaDomainCategoriesDao(SQLMediaDomainCategoriesSproc sproc) {
        this.sproc = sproc;
    }

    /**
     * Returns a List of Category Objects
     * @apiNote the Sproc only supports the lodging domain at the moment, hence this method only supports lodging domain
     *          we could make a switch case in the future
     *
     * @param domain        The Domain to query
     * @param localeId      The Localization Id to query by
     * @return List of Category Objects
     * @throws DomainNotFoundException
     */
    @SuppressWarnings("unchecked")
    public List<Category> getMediaCategoriesWithSubCategories(String domain, String localeId) throws DomainNotFoundException {
        if ("lodging".equals(domain.toLowerCase(Locale.US))) {
            return getLodgingMediaCategoriesWithSubCategories(localeId);
        } else {
            throw new DomainNotFoundException("Domain Not Found");
        }
    }

    /**
     * Using the Result Sets of SQLMediaDomainCategoriesSproc this Method groups the results by Category ID and then
     * populates each Category with Localized Names and SubCategories
     * @see Category, LocalizedName, SubCategory, SQLMediaDomainCategoriesSproc
     *
     * @param localeId  The Localization Id to query by
     * @return  List of Category Objects
     */
    @SuppressWarnings("unchecked")
    private List<Category> getLodgingMediaCategoriesWithSubCategories(String localeId) {
        final Map<String, Object> results = sproc.execute(localeId);
        final List<MediaCategory> mediaCategories = (List<MediaCategory>) results.get(SQLMediaDomainCategoriesSproc.MEDIA_CATEGORY_RESULT_SET);
        final List<MediaSubCategory> mediaSubCategories = (List<MediaSubCategory>) results.get(SQLMediaDomainCategoriesSproc.MEDIA_SUB_CATEGORY_RESULT_SET);

        final Map<Integer, List<MediaCategory>> categoryMap = mediaCategories.stream()
                .filter(category -> Integer.parseInt(category.getMediaCategoryID()) > SKIP_NULL_AND_DELETED_CATEGORIES)
                .collect(Collectors.groupingBy(category -> Integer.parseInt(category.getMediaCategoryID())));
        final Map<Integer, List<MediaSubCategory>> subCategoryMap = mediaSubCategories.stream()
                .collect(Collectors.groupingBy(subCategory -> Integer.parseInt(subCategory.getMediaCategoryID())));
        final List<Category> categoriesList = categoryMap.keySet().stream()
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
        if (subCategoryMap.get(categoryId) == null) {
            return new ArrayList<>();
        } else {
            final Map<Integer, List<MediaSubCategory>> innerSubCategoryMap = subCategoryMap.get(categoryId).stream()
                    .collect(Collectors.groupingBy(subCategory -> Integer.parseInt(subCategory.getMediaSubCategoryID())));
            final List<SubCategory> subCategoriesList = innerSubCategoryMap.keySet().stream()
                    .map(subCategoryId -> new SubCategory(String.valueOf(subCategoryId), innerSubCategoryMap.get(subCategoryId).stream()
                            .map(item -> new LocalizedName(item.getMediaSubCategoryName(), item.getLangID()))
                            .collect(Collectors.toList())))
                    .collect(Collectors.toList());
            return subCategoriesList;
        }
    }

    /**
     * verifies subCategory in the message
     * @param outerDomain
     * @param localeId
     * @return
     */
    public Boolean subCategoryIdExists(OuterDomain outerDomain, String localeId) {
        final String category = getCategory(outerDomain);
        Boolean categoryExists = Boolean.TRUE;
        if (outerDomain.getDomain().equals(Domain.LODGING)&& !"" .equals(category)) {
                final Map<String, Object> results = sproc.execute(localeId);
                final List<MediaSubCategory> mediaSubCategories = (List<MediaSubCategory>) results.get(SQLMediaDomainCategoriesSproc.MEDIA_SUB_CATEGORY_RESULT_SET);

                final List<String> subCategoryIds = mediaSubCategories.stream()
                        .map(s -> s.getMediaSubCategoryID())
                        .collect(Collectors.toList());

                categoryExists = subCategoryIds.contains(category);
        }
        return categoryExists;
    }

    /**
     * extracts category
     * @param outerDomain
     * @return
     */
    private String getCategory(OuterDomain outerDomain) {
        final String category = outerDomain.getDomainFields() == null ||
                outerDomain.getDomainFields().get(CATEGORY) == null ? "" :
                outerDomain.getDomainFields().get(CATEGORY).toString();
        return category;
    }
}
