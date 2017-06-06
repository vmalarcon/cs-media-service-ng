package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.dao.domain.Category;
import com.expedia.content.media.processing.services.dao.domain.Comment;
import com.expedia.content.media.processing.services.dao.domain.DomainCategory;
import com.expedia.content.media.processing.services.dao.domain.DomainIdMedia;
import com.expedia.content.media.processing.services.dao.domain.LocalizedName;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.Subcategory;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Util class for MediaDB SQL queries
 */
@SuppressWarnings({"PMD.UnsynchronizedStaticDateFormatter"})
public final class MediaDBSQLUtil {
    private static final FormattedLogger LOGGER = new FormattedLogger(MediaDBSQLUtil.class);
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS XXX", Locale.US);
    private static final String MEDIA_BY_DOMAIN_ID_ACTIVE_FILTER = " AND `active` = ?";
    private static final String MEDIA_BY_DOMAIN_ID_DERIVATIVE_CATEGORY_FILTER = " AND `derivative-category` IN (?)";
    private static final String MEDIA_BY_DOMAIN_ID_LIMIT = " LIMIT ?, ?";

    private MediaDBSQLUtil() {
        // no-op
    }

    /**
     * A helper method to set elements of an Array into a PreparedStatement.
     *
     * @param statement The PreparedStatement to set values in.
     * @param startIndex The start index to start setting elements.
     * @param array The array of elements to set int he PreparedStatement.
     * @return The final index +1 for setting elements after the array.
     */
    public static int setArray(PreparedStatement statement, final int startIndex, String[] array) {
        int index = startIndex;
        for(final String el : array) {
            try {
                statement.setString(index, el);
                ++index;
            } catch (Exception e) {
                LOGGER.error("couldn't set element={} from array={}", el, Arrays.toString(array));
            }
        }
        return index;
    }

    /**
     * A helper method to set tokens for an array of elements in a SQL Query String with \"IN (?)\".
     * i.e. for an array of 3 elements and a query string, it will do the following: "FROM table WHERE value IN (?)" -> "FROM table WHERE value IN (?,?,?)"
     *
     * @param sqlString The SQL Query string to add tokens to.
     * @param array The array to add tokens for.
     * @return The SQL Query String with new tokens added.
     */
    public static String setSQLTokensWithArray(String sqlString, String[] array) {
        final StringBuilder tokens = new StringBuilder();
        char delimiter = '\0';
        for (int i = 0; i < array.length; i++) {
            if (delimiter == '\0') {
                tokens.append('?');
                delimiter = ',';
            } else {
                tokens.append(delimiter).append('?');
            }
        }
        return StringUtils.replace(sqlString, "IN (?)", "IN (" + tokens.toString() + ")");
    }

    /**
     * LcmMediaId's need to be strings (to adhere to the API contract), this method looks in the DomainFields and
     * asserts that it is a String.
     *
     * @param domainFieldsMap The map of DomainField data.
     * @return The Map of DomainField data with LcmMediaId as a String.
     */
    public static Map<String, Object> convertLcmMediaIdInMapToString(Map<String, Object> domainFieldsMap) {
        if (domainFieldsMap != null && domainFieldsMap.get("lcmMediaId") != null) {
            final String lcmMediaId = String.valueOf(domainFieldsMap.get("lcmMediaId"));
            domainFieldsMap.put("lcmMediaId", lcmMediaId);
        }
        return domainFieldsMap;
    }

    /**
     * Converts a List of DomainCategory Objects to a Formatted list of Category Objects with lists of Subcategories.
     *
     * @param domainCategoryList The List of DomainCategory objects to convert and format.
     * @return A complete list of Categories.
     */
    public static List<Category> domainCategoryListToCategoryList(List<DomainCategory> domainCategoryList) {
        final Map<String, Category> categoryMap = new HashMap<>();
        domainCategoryList.forEach(domainCategory -> addDomainCategoryToMap(domainCategory, categoryMap));
        return categoryMap.values().stream()
                .map(category -> {
                    category.getSubcategories().sort(Comparator.comparing(Subcategory::getSubcategoryId));
                    category.getCategoryName().sort(Comparator.comparing(LocalizedName::getLocaleId));
                    return category;
                })
                .sorted(Comparator.comparing(Category::getCategoryId))
                .collect(Collectors.toList());
    }

    /**
     * Converts a DomainCategory object to Category or Subcategory Objects and adds it to the categoryMap.
     * The logic is a little complex at first, so here's how it works:
     * 1) If a DomainCategory Object has a parentCategoryId, it is Subcategory. Otherwise it is a Category.
     * 2) Different DomainCategories share the same CategoryId (or SubcategoryId), so they must map to the same CategoryId (or SubcategoryId).
     *   2.a) If a map for categoryId is null, a new Category is built. Otherwise the LocalizedName of the DomainCategory is added to the existing categoryMap,
     *        and an Empty List is set for the Category Object.
     *   2.b) A subcategory must be added to a Category Object, if one does not exist yet a new Category Object is created, with an emtpy list of LocalizedNames.
     * @see DomainCategory
     * @see Category
     * @see Subcategory
     * @see LocalizedName
     *
     * @param domainCategory The DomainCategory to convert, and add to the categoryMap.
     * @param categoryMap The Map to added the Converted Category to.
     */
    private static void addDomainCategoryToMap(DomainCategory domainCategory, Map<String, Category> categoryMap) {
        // Check if domainCategory is a Category (if parentCategory is null), or a Subcategory
        if (domainCategory.getParentCategoryId() == null) {
            final Category category = categoryMap.get(domainCategory.getCategoryId());
            // Check to see whether to create a new Category, or add the LocalizedName to the CategoryName list.
            if (category == null) {
                final LocalizedName localizedName = new LocalizedName(domainCategory.getLocalizedName(), domainCategory.getLocaleId());
                categoryMap.put(domainCategory.getCategoryId(), new Category(domainCategory.getCategoryId(), new ArrayList<>(Arrays.asList(localizedName)), new ArrayList<>()));
            } else {
                category.getCategoryName().add(new LocalizedName(domainCategory.getLocalizedName(), domainCategory.getLocaleId()));
            }
        } else {
            final Category category = categoryMap.get(domainCategory.getParentCategoryId());
            // Check if the parentCategory of the Subcategory Exists, if not create a new Category Object to add the Subcategory to.
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
                // Check if a SubcategoryId exists in the Category's subcategoryId list, if not add a new Subcategory. Otherwise add the LocalizedName to the SubcategoryList.
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

    /**
     * Builds a Media from a ResultSet returned from a query.
     *
     * @param resultSet A result from a query.
     * @return a Media object of a ResultSet.
     */
    public static Optional<Media> buildMediaFromResultSet(ResultSet resultSet) {
        try {
            final String domainFields = resultSet.getString("domain-fields");
            final Map<String, Object> domainData = convertLcmMediaIdInMapToString(JSONUtil.buildMapFromJson(domainFields));
            final String derivatives = resultSet.getString("derivatives");
            final String fingerprints = resultSet.getString("fingerprints");
            final List<Map<String, Object>> fingerprintsList = JSONUtil.buildMapListFromJson(fingerprints);
            final String pHash = fingerprintsList.stream().filter(map -> "pHash".equals(map.get("algorithm")))
                    .map(map -> ((List<String>) map.get("values")).get(0)).findFirst().orElse(null);
            final String sha1 = fingerprintsList.stream().filter(map -> "SHA1".equals(map.get("algorithm")))
                    .map(map -> ((List<String>) map.get("values")).get(0)).findFirst().orElse(null);
            final Media media = Media.builder()
                    .mediaGuid(resultSet.getString("guid"))
                    .fileUrl(resultSet.getString("file-url"))
                    .fileName(resultSet.getString("file-name"))
                    .fileSize((long) resultSet.getInt("file-size"))
                    .width(resultSet.getInt("width"))
                    .height(resultSet.getInt("height"))
                    .sourceUrl(resultSet.getString("source-url"))
                    .domain(resultSet.getString("domain"))
                    .domainId(resultSet.getString("domain-id"))
                    .domainFields(domainFields)
                    .lastUpdated(resultSet.getTimestamp("update-date"))
                    .active((resultSet.getInt("active") == 1) ? Boolean.TRUE.toString() : Boolean.FALSE.toString())
                    .provider(resultSet.getString("provider"))
                    .clientId(resultSet.getString("client-id"))
                    .userId(resultSet.getString("user-id"))
                    .metadata(resultSet.getString("metadata"))
                    .derivatives(derivatives)
                    .pHash(pHash)
                    .sha1(sha1)
                    .environment(null)
                    .lcmMediaId(domainData.get("lcmMediaId") == null ? "" : domainData.get("lcmMediaId").toString())
                    .derivativesList(JSONUtil.buildMapListFromJson(derivatives))
                    .domainData(domainData)
                    .commentList(Arrays.asList(resultSet.getString("comments")))
                    .status(resultSet.getString("status"))
                    .domainDerivativeCategory(resultSet.getString("derivative-category"))
                    .propertyHero(Boolean.valueOf((String) domainData.get("propertyHero")))
                    .hidden(resultSet.getInt("hidden") == 1)
                    .providedName(resultSet.getString("provided-name"))
                    .build();
            return Optional.of(media);
        } catch (SQLException e) {
            LOGGER.error(e, "Error querying MediaDB result-set={}", resultSet.toString());
            return Optional.empty();
        }
    }

    /**
     * Adds extra WHERE clauses to an SQL Query String.
     *
     * @param baseQuery                         The Base SQL query string to append to.
     * @param isActiveFilterUsed                A flag to filter records by active status.
     * @param isDerivativeCategoryFilterUsed    A flag to filter derivative categories.
     * @param isPaginationUsed                  A flag to limit the record count.
     * @param derivativeCategoryFilterArray     An array of derivative types to query for.
     * @return A complete SQL Query based on the flags passed to the method.
     */
    public static String setMediaByDomainIdQueryString(String baseQuery, boolean isActiveFilterUsed, boolean isDerivativeCategoryFilterUsed,
                                                       boolean isPaginationUsed, String[] derivativeCategoryFilterArray ) {
        final StringBuilder finalQuery = new StringBuilder(baseQuery);
        if (isActiveFilterUsed) {
            finalQuery.append(MEDIA_BY_DOMAIN_ID_ACTIVE_FILTER);
        }
        if (isDerivativeCategoryFilterUsed) {
            finalQuery.append(setSQLTokensWithArray(MEDIA_BY_DOMAIN_ID_DERIVATIVE_CATEGORY_FILTER, derivativeCategoryFilterArray));

        }
        if (isPaginationUsed) {
            finalQuery.append(MEDIA_BY_DOMAIN_ID_LIMIT);
        }
        return finalQuery.toString();
    }


    /**
     * Builds a DomainIdMedia from a result set query.
     *
     * @param resultSet              The resultSet to extract fields from.
     * @param isDerivativeFilterUsed a flag to decide whether to filter derivatives returned in the result set.
     * @param derivativeFilter       The filter to determine which derivatives to keep in the result set.
     * @return An Optional DomainIdMedia object. If the resultSet is malformed an Option.empty() object is returned.
     */
    public static Optional<DomainIdMedia> buildDomainIdMediaFromResultSet(ResultSet resultSet, boolean isDerivativeFilterUsed, String derivativeFilter) {
        try {
            final DomainIdMedia domainIdMedia = DomainIdMedia.builder()
                    .mediaGuid(resultSet.getString("guid"))
                    .fileUrl(resultSet.getString("file-url"))
                    .sourceUrl(resultSet.getString("source-url"))
                    .fileName(resultSet.getString("file-name"))
                    .active((resultSet.getInt("active") == 1) ? Boolean.TRUE.toString() : Boolean.FALSE.toString())
                    .width(resultSet.getInt("width"))
                    .height(resultSet.getInt("height"))
                    .fileSize((long) resultSet.getInt("file-size"))
                    .status(resultSet.getString("status"))
                    .lastUpdatedBy(resultSet.getString("updated-by"))
                    .lastUpdateDateTime(DATE_FORMAT.format(resultSet.getTimestamp("update-date")))
                    .domainProvider(resultSet.getString("provider"))
                    .domainDerivativeCategory(resultSet.getString("derivative-category"))
                    .domainFields(convertLcmMediaIdInMapToString(JSONUtil.buildMapFromJson(resultSet.getString("domain-fields"))))
                    .derivatives(resultSet.getString("derivatives") == null ? null :
                            (isDerivativeFilterUsed ? filterDerivatives(resultSet.getString("derivatives"), derivativeFilter) :
                                    JSONUtil.buildMapListFromJson(resultSet.getString("derivatives")))
                    )
                    .comments(Arrays.asList(Comment.builder()
                            .note(resultSet.getString("comments"))
                            .timestamp(DATE_FORMAT.format(resultSet.getTimestamp("update-date")))
                            .build()))
                    .build();
            return Optional.of(domainIdMedia);
        } catch (SQLException e) {
            LOGGER.error(e, "Error querying MediaDB result-set={}", resultSet.toString());
            return Optional.empty();
        }
    }

    /**
     * Filters Derivatives by the derivative filter and returns a List of Maps representing Derivatives.
     *
     * @param jsonString The JSON String containing the derivatives data from the MediaDB.
     * @param derivativeFilter The String containing a comma separated list of wanted derivative types.
     * @return A List of Maps of Derivatives with the wanted derivative types.
     */
    @SuppressWarnings("CPD-END")
    private static List<Map<String, Object>> filterDerivatives(String jsonString, String derivativeFilter) {
        final List<String> derivativeTypes = Arrays.asList(derivativeFilter.split(","));
        final List<Map<String, Object>> derivatives = JSONUtil.buildMapListFromJson(jsonString);
        return derivatives.stream()
                .filter(map -> derivativeTypes.contains((String) map.get("type")))
                .collect(Collectors.toList());
    }
}
