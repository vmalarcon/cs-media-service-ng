package com.expedia.content.media.processing.services.dao.mediadb;

import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.DomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.MediaSubCategoryCache;
import com.expedia.content.media.processing.services.dao.domain.Category;
import com.expedia.content.media.processing.services.dao.domain.DomainCategory;
import com.expedia.content.media.processing.services.dao.domain.LocalizedName;
import com.expedia.content.media.processing.services.dao.domain.Subcategory;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MediaDBDomainCategoriesDao implements DomainCategoriesDao {
    private MediaSubCategoryCache mediaSubCategoryCache;
    private static final String SUBCATEGORY_ID = "subcategoryId";
    private static final String ALL_CATEGORIES_AND_SUBCATEGORIES_QUERY = "SELECT * FROM `domain-category` WHERE `domain` = ?";
    private static final String CATEGORIES_AND_SUBCATEGORIES_BY_LOCAL_ID_QUERY = "SELECT * FROM `domain-category` WHERE `domain` = ? AND `locale-id` = ?";

    private final JdbcTemplate jdbcTemplate;

    public MediaDBDomainCategoriesDao(DataSource mediaDBDataSource) {
        this.jdbcTemplate = new JdbcTemplate(mediaDBDataSource);
    }

    @Override
    public List<Category> getMediaCategoriesWithSubCategories(String domain, String localeId) {
        return (localeId == null) ? getAllMediaCategoriesWithSubCategories(domain) : getMediaCategoriesWithSubCategoriesByLocaleId(domain, localeId);
    }

    @SuppressWarnings("CPD-START")
    private List<Category> getAllMediaCategoriesWithSubCategories(String domain) {
        final Map<String, Category> categoryMap = new HashMap<>();
        jdbcTemplate.query((Connection connection) -> {
            final PreparedStatement statement = connection.prepareStatement(ALL_CATEGORIES_AND_SUBCATEGORIES_QUERY);
            statement.setString(1, domain);
            return statement;
        }, (ResultSet resultSet, int rowNumb) -> {
            final String parentCategoryId = resultSet.getString("parent-category-id");
            final String categoryId = resultSet.getString("category-id");
            final String localeId = resultSet.getString("locale-id");
            final String localizedName = resultSet.getString("localized-name");
            return new DomainCategory(parentCategoryId, categoryId, localeId, localizedName);
        }).forEach(domainCategory -> addDomainCategoryToMap(domainCategory, categoryMap));
        return categoryMap.values().stream().map(category -> {
            category.getSubcategories().sort(Comparator.comparing(Subcategory::getSubcategoryId));
            category.getCategoryName().sort(Comparator.comparing(LocalizedName::getLocaleId));
            return category;
        }).sorted(Comparator.comparing(Category::getCategoryId)).collect(Collectors.toList());
    }

    private List<Category> getMediaCategoriesWithSubCategoriesByLocaleId(String domain, String queryLocaleId) {
        final Map<String, Category> categoryMap = new HashMap<>();
        jdbcTemplate.query((Connection connection) -> {
            final PreparedStatement statement = connection.prepareStatement(CATEGORIES_AND_SUBCATEGORIES_BY_LOCAL_ID_QUERY);
            statement.setString(1, domain);
            statement.setString(2, queryLocaleId);
            return statement;
        }, (ResultSet resultSet, int rowNumb) -> {
            final String parentCategoryId = resultSet.getString("parent-category-id");
            final String categoryId = resultSet.getString("category-id");
            final String localeId = resultSet.getString("locale-id");
            final String localizedName = resultSet.getString("localized-name");
            return new DomainCategory(parentCategoryId, categoryId, localeId, localizedName);
        }).forEach(domainCategory -> addDomainCategoryToMap(domainCategory, categoryMap));
        return categoryMap.values().stream().map(category -> {
            category.getSubcategories().sort(Comparator.comparing(Subcategory::getSubcategoryId));
            category.getCategoryName().sort(Comparator.comparing(LocalizedName::getLocaleId));
            return category;
        }).sorted(Comparator.comparing(Category::getCategoryId)).collect(Collectors.toList());
    }

    @SuppressWarnings("CPD-END")
    private void addDomainCategoryToMap(DomainCategory domainCategory, Map<String, Category> categoryMap) {
        if (domainCategory.getParentCategoryId() == null) {
            final Category category = categoryMap.get(domainCategory.getCategoryId());
            if (category == null) {
                final LocalizedName localizedName = new LocalizedName(domainCategory.getLocalizedName(), domainCategory.getLocaleId());
                categoryMap.put(domainCategory.getCategoryId(), new Category(domainCategory.getCategoryId(), new ArrayList<>(Arrays.asList(localizedName)), new ArrayList<>()));
            } else {
                category.getCategoryName().add(new LocalizedName(domainCategory.getLocalizedName(), domainCategory.getLocaleId()));
            }
        } else {
            final Category category = categoryMap.get(domainCategory.getParentCategoryId());
            if (category == null) {
                final LocalizedName localizedName = new LocalizedName(domainCategory.getLocalizedName(), domainCategory.getLocaleId());
                final Subcategory subcategory = new Subcategory(domainCategory.getCategoryId(), new ArrayList<>(Arrays.asList(localizedName)));
                categoryMap.put(domainCategory.getParentCategoryId(), new Category(domainCategory.getParentCategoryId(), new ArrayList<>(),
                        new ArrayList<>(Arrays.asList(subcategory))));
            } else {
                Subcategory subcategory = category.getSubcategories().stream()
                        .filter(subcat -> domainCategory.getCategoryId().equals(subcat.getSubcategoryId()))
                        .findFirst()
                        .orElse(null);
                if (subcategory == null) {
                    final LocalizedName localizedName = new LocalizedName(domainCategory.getLocalizedName(), domainCategory.getLocaleId());
                    subcategory = new Subcategory(domainCategory.getCategoryId(), new ArrayList<>(Arrays.asList(localizedName)));
                    category.getSubcategories().add(subcategory);
                } else {
                    final LocalizedName localizedName = new LocalizedName(domainCategory.getLocalizedName(), domainCategory.getLocaleId());
                    subcategory.getSubcategoryName().add(localizedName);
                }
            }
        }
    }

    @Override
    public Boolean subCategoryIdExists(OuterDomain outerDomain, String localeId) {
        final String category = getCategory(outerDomain);
        Boolean categoryExists = Boolean.TRUE;
        if (StringUtils.isNotBlank(category)) {
            final Calendar cal = Calendar.getInstance();
            if (mediaSubCategoryCache == null || mediaSubCategoryCache.getExpiryDate().before(cal.getTime())) {
                final List<Category> domainCategories = getMediaCategoriesWithSubCategoriesByLocaleId(outerDomain.getDomain().getDomain(), localeId);
                setMediaSubCategoryCache(domainCategories, cal);
            }
            categoryExists = mediaSubCategoryCache.getMediaSubCategoryCache().contains(category);
        }
        return categoryExists;
    }

    /**
     * extracts category
     *
     * @param outerDomain
     * @return
     */
    private String getCategory(OuterDomain outerDomain) {
        final String category = outerDomain.getDomainFields() == null ||
                outerDomain.getDomainFields().get(SUBCATEGORY_ID) == null ? "" :
                outerDomain.getDomainFields().get(SUBCATEGORY_ID).toString();
        return category;
    }

    /**
     * sets mediaSubCategoryCache which contains the subcategories
     * @param categoryList
     * @param expiryDate
     */
    private void setMediaSubCategoryCache(List<Category> categoryList, Calendar expiryDate) {
        final List<String> mediaSubCategoryIds = new ArrayList<>();
        for (final Category category : categoryList) {
            for (final Subcategory subcategory : category.getSubcategories()) {
                mediaSubCategoryIds.add(subcategory.getSubcategoryId());
            }
        }
        expiryDate.add(Calendar.DATE, 1);
        mediaSubCategoryCache = new MediaSubCategoryCache(mediaSubCategoryIds, expiryDate.getTime());
    }
}
