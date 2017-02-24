package com.expedia.content.media.processing.services.dao.mediadb;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.reqres.Comment;
import com.expedia.content.media.processing.services.reqres.DomainIdMedia;
import com.expedia.content.media.processing.services.reqres.MediaByDomainIdResponse;
import com.expedia.content.media.processing.services.reqres.MediaGetResponse;
import com.expedia.content.media.processing.services.util.JSONUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Media data access operations through MediaDB.
 */
@Component
public class MediaDBMediaDao implements MediaDao {

    private static final FormattedLogger LOGGER = new FormattedLogger(MediaDBMediaDao.class);
    private static final String ACTIVE_FILTER_ALL = "all";
    private static final String ACTIVE_FILTER_TRUE = "true";
    private static final String MEDIA_BY_DOMAIN_ID_QUERY_BASE = "SELECT SQL_CALC_FOUND_ROWS * FROM `media` WHERE `domain` = ? AND `domain-id` = ? AND `hidden` = 0";
    private static final String MEDIA_BY_DOMAIN_ID_ACTIVE_FILTER = " AND `active` = ?";
    private static final String MEDIA_BY_DOMAIN_ID_DERIVATIVE_CATEGORY_FILTER = " AND `derivative-category` IN (?)";
    private static final String MEDIA_BY_DOMAIN_ID_LIMIT = " LIMIT ?, ?";
    private static final String TOTAL_ROWS_QUERY = "SELECT COUNT(*) FROM `media` WHERE `domain` = ? AND `domain-id` = ? AND `hidden` = 0";
    private static final String MEDIA_BY_FILE_NAME_QUERY = "SELECT * FROM `media` WHERE `file-name` = ?";
    private static final String MEDIAS_BY_FILE_NAMES_QUERY = "SELECT * FROM `media` WHERE `file-name` IN (?)";
    private static final String MEDIA_BY_GUID_QUERY = "SELECT * FROM `media` WHERE `guid` = ?";
    private static final String DELETE_MEDIA_BY_GUID = "DELETE FROM `media` WHERE `guid` = ?";
    private static final String MEDIA_BY_LCM_MEDIA_ID = "SELECT * FROM `media` WHERE `domain-fields` LIKE ?";

    private final JdbcTemplate jdbcTemplate;

    public MediaDBMediaDao(DataSource mediaDBDataSource) {
        this.jdbcTemplate = new JdbcTemplate(mediaDBDataSource);
    }

    @Override
    @SuppressWarnings("CPD-START")
    public MediaByDomainIdResponse getMediaByDomainId(Domain domain, String domainId, String activeFilter, String derivativeFilter,
                                                      String derivativeCategoryFilter, Integer pageSize, Integer pageIndex) throws Exception {
        final boolean isActiveFilterUsed = activeFilter != null && !activeFilter.isEmpty() && !ACTIVE_FILTER_ALL.equals(activeFilter);
        final boolean isDerivativeCategoryFilterUsed = derivativeCategoryFilter != null && !derivativeCategoryFilter.isEmpty();
        final boolean isPaginationUsed = pageSize != null && pageIndex != null;
        final boolean isDerivativeFilterUsed = derivativeFilter != null && !derivativeFilter.isEmpty();
        final StringBuilder getMediaByDomainIdQuery = new StringBuilder(MEDIA_BY_DOMAIN_ID_QUERY_BASE);
        final StringBuilder totalRowsQuery = new StringBuilder(TOTAL_ROWS_QUERY);
        final String[] derivativeCategoryFilterArray = isDerivativeCategoryFilterUsed ? derivativeCategoryFilter.split(",") : null;
        if (isActiveFilterUsed) {
            getMediaByDomainIdQuery.append(MEDIA_BY_DOMAIN_ID_ACTIVE_FILTER);
            totalRowsQuery.append(MEDIA_BY_DOMAIN_ID_ACTIVE_FILTER);
        }
        if (isDerivativeCategoryFilterUsed) {
            getMediaByDomainIdQuery.append(setSQLTokensWithArray(MEDIA_BY_DOMAIN_ID_DERIVATIVE_CATEGORY_FILTER, derivativeCategoryFilterArray));
            totalRowsQuery.append(MEDIA_BY_DOMAIN_ID_DERIVATIVE_CATEGORY_FILTER);
        }
        if (isPaginationUsed) {
            getMediaByDomainIdQuery.append(MEDIA_BY_DOMAIN_ID_LIMIT);
        }
        final List<DomainIdMedia> domainIdMedias = jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(getMediaByDomainIdQuery.toString());
            statement.setString(1, domain.getDomain());
            statement.setString(2, domainId);
            int additionalIndex = 3;
            if (isActiveFilterUsed) {
                statement.setInt(additionalIndex, ((ACTIVE_FILTER_TRUE).equals(activeFilter) ? 1 : 0));
                additionalIndex++;
            }
            if (isDerivativeCategoryFilterUsed) {
                additionalIndex = setArray(statement, additionalIndex, derivativeCategoryFilterArray);
            }
            if (isPaginationUsed) {
                int start = pageSize * (pageIndex - 1);
                statement.setInt(additionalIndex, start);
                statement.setInt(additionalIndex + 1, pageSize);
            }
            return statement;
        }, (ResultSet resultSet, int rowNumb) ->
                DomainIdMedia.builder()
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
                .lastUpdateDateTime(resultSet.getTimestamp("update-date").toString())
                .domainProvider(resultSet.getString("provider"))
                .domainDerivativeCategory(resultSet.getString("derivative-category"))
                .domainFields(JSONUtil.buildMapFromJson(resultSet.getString("domain-fields")))
                .derivatives(resultSet.getString("derivatives") != null ?
                        (isDerivativeFilterUsed ? filterDerivatives(resultSet.getString("derivatives"), derivativeFilter) :
                        JSONUtil.buildMapListFromJson(resultSet.getString("derivatives"))) : null
                )
                .comments(Arrays.asList(Comment.builder()
                        .note(resultSet.getString("comments"))
                        .timestamp(resultSet.getTimestamp("update-date").toString())
                        .build()))
                .build()).stream().collect(Collectors.toList());

        // Second Query to get total media for the domainId,
        final Integer totalMediaCount = jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(totalRowsQuery.toString());
            statement.setString(1, domain.getDomain());
            statement.setString(2, domainId);
            int additionalIndex = 3;
            if (isActiveFilterUsed) {
                statement.setInt(additionalIndex, ((ACTIVE_FILTER_TRUE).equals(activeFilter) ? 1 : 0));
                additionalIndex++;
            }
            if (isDerivativeCategoryFilterUsed) {
                setArray(statement, additionalIndex, derivativeCategoryFilterArray);
            }
            return statement;
        },
                (ResultSet resultSet) -> resultSet.next() ? resultSet.getInt(1) : null);

        return MediaByDomainIdResponse.builder()
                .domain(domain.getDomain())
                .domainId(domainId)
                .totalMediaCount(totalMediaCount)
                .images(domainIdMedias)
                .build();
    }


    /**
     * Filters Derivatives by the derivative filter and returns a List of Maps representing Derivatives.
     *
     * @param jsonString The JSON String containing the derivatives data from the MediaDB.
     * @param derivativeFilter The String containing a comma separated list of wanted derivative types.
     * @return A List of Maps of Derivatives with the wanted derivative types.
     */
    @SuppressWarnings("CPD-END")
    private List<Map<String, Object>> filterDerivatives(String jsonString, String derivativeFilter) {
        final List<String> derivativeTypes = Arrays.asList(derivativeFilter.split(","));
        final List<Map<String, Object>> derivatives = JSONUtil.buildMapListFromJson(jsonString);
        return derivatives.stream().filter(map -> derivativeTypes.contains((String) map.get("type"))).collect(Collectors.toList());
    }

    @Override
    public List<Media> getMediaByFilename(String fileName) {
        final List<Media> medias = jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(MEDIA_BY_FILE_NAME_QUERY);
            statement.setString(1, fileName);
            return statement;
        }, (ResultSet resultSet, int rowNumb) -> buildMediaFromResultSet(resultSet)).stream().collect(Collectors.toList());
        return medias;
    }

    @Override
    public List<LcmMedia> getMediaByFilenameInLCM(int domainId, String fileName) {
        return null;
    }

    @Override
    @SuppressWarnings("CPD-START")
    public MediaGetResponse getMediaByGUID(String mediaGUID) {
        return jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(MEDIA_BY_GUID_QUERY);
            statement.setString(1, mediaGUID);
            return statement;
        }, (ResultSet resultSet) -> resultSet.next() ? MediaGetResponse.builder()
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
                .lastUpdateDateTime(resultSet.getTimestamp("update-date").toString())
                .domainProvider(resultSet.getString("provider"))
                .domainDerivativeCategory(resultSet.getString("derivative-category"))
                .domainFields(JSONUtil.buildMapFromJson(resultSet.getString("domain-fields")))
                .derivatives(JSONUtil.buildMapListFromJson(resultSet.getString("derivatives")))
                .comments(Arrays.asList(Comment.builder()
                        .note(resultSet.getString("comments"))
                        .timestamp(resultSet.getTimestamp("update-date").toString())
                        .build()))
                .domain(resultSet.getString("domain"))
                .domainId(resultSet.getString("domain-id"))
                .build() : null);
    }

    @Override
    @SuppressWarnings("CPD-END")
    public void deleteMediaByGUID(String mediaGUID) {
        jdbcTemplate.update((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(DELETE_MEDIA_BY_GUID);
            statement.setString(1, mediaGUID);
            return statement;
        });
    }

    @Deprecated
    @Override
    public LcmMedia getContentProviderName(String fileName) {
        return null;
    }

    @Override
    public Media getMediaByGuid(String guid) {
        return jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(MEDIA_BY_GUID_QUERY);
            statement.setString(1, guid);
            return statement;
        }, (ResultSet resultSet) -> resultSet.next() ? buildMediaFromResultSet(resultSet) : null);
    }

    @Override
    public List<Media> getMediaByMediaId(String mediaId) {
        final String lcmMediaIdSubstring = "%\"lcmMediaId\":\"" + mediaId + "\"%";
        List<Media> medias = jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(MEDIA_BY_LCM_MEDIA_ID);
            statement.setString(1, lcmMediaIdSubstring);
            return statement;
        }, (ResultSet resultSet, int rowNumb) -> buildMediaFromResultSet(resultSet)).stream().collect(Collectors.toList());
        return medias;
    }

    @Deprecated
    @Override
    public void saveMedia(Media media) {
        // no-op
    }

    @Override
    public Map<String, String> getLatestStatus(List<String> fileNames) {
        final String[] fileNamesArray = fileNames.toArray(new String[fileNames.size()]);
        Map<String, String> statusMap = new HashMap<>();
        jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(setSQLTokensWithArray(MEDIAS_BY_FILE_NAMES_QUERY, fileNamesArray));
            setArray(statement, 1, fileNamesArray);
            return statement;
        }, (ResultSet resultSet, int rowNumb) -> statusMap.put(resultSet.getString("file-name"), resultSet.getString("status")));
        return statusMap;
    }

    /**
     * Builds a Media from a ResultSet returned form a query.
     *
     * @param resultSet A result from a query.
     * @return a Media object of a ResultSet.
     */
    private Media buildMediaFromResultSet(ResultSet resultSet) {
        try {
            final String domainFields = resultSet.getString("domain-fields");
            final Map<String, Object> domainData = JSONUtil.buildMapFromJson(domainFields);
            final String derivatives = resultSet.getString("derivatives");
            final String fingerprints = resultSet.getString("fingerprints");
            final List<Map<String, Object>> fingerprintsList = JSONUtil.buildMapListFromJson(fingerprints);
            final String pHash = fingerprintsList.stream().filter(map -> "pHash".equals(map.get("algorithm")))
                    .map(map -> ((List<String>) map.get("values")).get(0)).findFirst().orElse(null);
            final String sha1 = fingerprintsList.stream().filter(map -> "SHA1".equals(map.get("algorithm")))
                    .map(map -> ((List<String>) map.get("values")).get(0)).findFirst().orElse(null);
            return Media.builder()
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
                    .lcmMediaId(domainData.get("lcmMediaId").toString())
                    .derivativesList(JSONUtil.buildMapListFromJson(derivatives))
                    .domainData(domainData)
                    .commentList(Arrays.asList(resultSet.getString("comments")))
                    .status(resultSet.getString("status"))
                    .domainDerivativeCategory(resultSet.getString("derivative-category"))
                    .propertyHero(Boolean.valueOf((String) domainData.get("propertyHero")))
                    .hidden(resultSet.getInt("hidden") == 1)
                    .providedName(resultSet.getString("provided-name"))
                    .build();
        } catch (SQLException e) {
            LOGGER.error(e, "Error querying MediaDB result-set={}", resultSet.toString());
            return null;
        }
    }

    /**
     * A helper method to set elements of an Array into a PreparedStatement.
     *
     * @param statement The PreparedStatement to set values in.
     * @param startIndex The start index to start setting elements.
     * @param array The array of elements to set int he PreparedStatement.
     * @return The final index +1 for setting elements after the array.
     */
    private int setArray(PreparedStatement statement, int startIndex, String[] array) {
        for(String el : array) {
            try {
                statement.setString(startIndex, el);
                startIndex++;
            } catch (Exception e) {
                LOGGER.error("couldn't set element={} from array={}", el, Arrays.toString(array));
            }
        }
        return startIndex;
    }

    /**
     * A helper method to set tokens for an array of elements in a SQL Query String with \"IN (?)\".
     * i.e. for an array of 3 elements and a query string: FROM table WHERE value IN (?) -> FROM table WHERE value IN (?,?,?)
     *
     * @param sqlString The SQL Query string to add tokens to.
     * @param array The array to add tokens for.
     * @return The SQL Query String with new tokens added.
     */
    private String setSQLTokensWithArray(String sqlString, String[] array) {
        StringBuilder tokens = new StringBuilder();
        String delimiter = "";
        for(String el : array) {
            tokens.append(delimiter).append("?");
            delimiter = ",";
        }
        return StringUtils.replace(sqlString, "IN (?)", "IN (" + tokens.toString() + ")");
    }
}
