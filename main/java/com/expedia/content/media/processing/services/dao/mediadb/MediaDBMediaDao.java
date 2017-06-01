package com.expedia.content.media.processing.services.dao.mediadb;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.reporting.App;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.domain.DomainIdMedia;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.expedia.content.media.processing.services.dao.mediadb.MediaDBSQLUtil.buildDomainIdMediaFromResultSet;
import static com.expedia.content.media.processing.services.dao.mediadb.MediaDBSQLUtil.buildMediaFromResultSet;
import static com.expedia.content.media.processing.services.dao.mediadb.MediaDBSQLUtil.setArray;
import static com.expedia.content.media.processing.services.dao.mediadb.MediaDBSQLUtil.setMediaByDomainIdQueryString;
import static com.expedia.content.media.processing.services.dao.mediadb.MediaDBSQLUtil.setSQLTokensWithArray;

/**
 * Media data access operations through MediaDB.
 */
@Component
public class MediaDBMediaDao implements MediaDao {

    private static final FormattedLogger LOGGER = new FormattedLogger(MediaDBMediaDao.class);
    private static final ObjectWriter WRITER = new ObjectMapper().writer();
    private static final String ACTIVE_FILTER_ALL = "all";
    private static final String ACTIVE_FILTER_TRUE = "true";
    private static final String MEDIA_BY_DOMAIN_ID_QUERY_BASE = "SELECT SQL_CALC_FOUND_ROWS * FROM `media` WHERE `domain` = ? AND `domain-id` = ? AND `hidden` = 0";
    private static final String TOTAL_ROWS_QUERY = "SELECT COUNT(*) FROM `media` WHERE `domain` = ? AND `domain-id` = ? AND `hidden` = 0";
    private static final String MEDIA_BY_FILE_NAME_QUERY = "SELECT * FROM `media` WHERE `file-name` = ?";
    private static final String MEDIAS_BY_FILE_NAMES_QUERY = "SELECT * FROM `media` WHERE `file-name` IN (?)";
    private static final String MEDIA_BY_GUID_QUERY = "SELECT `guid`, `file-url`, `source-url`, `file-name`, `active`, `width`, `height`, `file-size`, `status`, `updated-by`, " +
            "`update-date`, `provider`, `derivative-category`, `domain-fields`, `provided-name`, `hidden`, `metadata`, `user-id`, `client-id`, `derivatives`,`fingerprints`, `comments`, `update-date`, `domain`, `domain-id` FROM `media` WHERE `guid` = ?";

    private static final String DELETE_MEDIA_BY_GUID = "DELETE FROM `media` WHERE `guid` = ?";
    private static final String MEDIA_BY_LCM_MEDIA_ID = "SELECT * FROM `media` WHERE `domain-fields` LIKE ?";
    private static final String ADD_WITH_IMAGEMESSAGEAVRO_QUERY = "INSERT INTO `media` " +
            "(`guid`, `file-url`, `file-name`, `active`, `client-id`, `user-id`, `hidden`, `updated-by`, `update-date`, " +
            "`domain`, `domain-id`, `provider`, `domain-fields`, `comments`, `provided-name`, `callback`)" +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_WITH_IMAGEMESSAGEAVRO_QUERY = "UPDATE `media` SET " +
            "`active` = IFNULL(?, active), " +
            "`client-id` = IFNULL(?, `client-id`), " +
            "`user-id` = ?, " +
            "`hidden` = IFNULL(?, hidden), " +
            "`updated-by` = ?, " +
            "`update-date` = ?, " +
            "`domain-fields` = IFNULL(?, `domain-fields`), " +
            "`comments` = IFNULL(?, comments), " +
            "`domain` = ?, " +
            "`domain-id` = ?, " +
            "`provider` = IFNULL(?, provider), " +
            "`file-url` = IFNULL(?, `file-url`), " +
            "`file-name` = IFNULL(?, `file-name`), " +
            "`provided-name` = IFNULL(?, `provided-name`), " +
            "`callback` = IFNULL(?, callback) " +
            "WHERE `guid` = ?";

    private static final String HERO_MEDIA_QUERY = "SELECT * FROM `media` WHERE `domain` = ? AND `domain-id` = ? AND `domain-fields` like '%\"propertyHero\":\"true\"%' ";

    private static final String UPDATE_MEDIA_UNHERO_QUERY = "UPDATE `media` SET " + " `domain-fields` = IFNULL(?, `domain-fields`) WHERE `guid` = ?";

    private final JdbcTemplate jdbcTemplate;

    public MediaDBMediaDao(DataSource mediaDBDataSource) {
        this.jdbcTemplate = new JdbcTemplate(mediaDBDataSource);
    }

    @Override
    @SuppressWarnings("CPD-START")
    public List<Optional<DomainIdMedia>> getMediaByDomainId(Domain domain, String domainId, String activeFilter, String derivativeFilter,
                                                      String derivativeCategoryFilter, Integer pageSize, Integer pageIndex) {
        final boolean isActiveFilterUsed = activeFilter != null && !activeFilter.isEmpty() && !ACTIVE_FILTER_ALL.equals(activeFilter);
        final boolean isDerivativeCategoryFilterUsed = derivativeCategoryFilter != null && !derivativeCategoryFilter.isEmpty();
        final boolean isPaginationUsed = pageSize != null && pageIndex != null;
        final boolean isDerivativeFilterUsed = derivativeFilter != null && !derivativeFilter.isEmpty();
        final String[] derivativeCategoryFilterArray = isDerivativeCategoryFilterUsed ? derivativeCategoryFilter.split(",") : null;
        final String getMediaByDomainIdQuery = setMediaByDomainIdQueryString(MEDIA_BY_DOMAIN_ID_QUERY_BASE, isActiveFilterUsed, isDerivativeCategoryFilterUsed,
                isPaginationUsed, derivativeCategoryFilterArray);
        return jdbcTemplate.query((Connection connection) -> {
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
                buildDomainIdMediaFromResultSet(resultSet, isDerivativeFilterUsed, derivativeFilter))
        .stream()
        .collect(Collectors.toList());
    }

    public Optional<Integer> getTotalMediaCountByDomainId(Domain domain, String domainId, String activeFilter, String derivativeCategoryFilter) {
        final boolean isActiveFilterUsed = activeFilter != null && !activeFilter.isEmpty() && !ACTIVE_FILTER_ALL.equals(activeFilter);
        final boolean isDerivativeCategoryFilterUsed = derivativeCategoryFilter != null && !derivativeCategoryFilter.isEmpty();
        final String[] derivativeCategoryFilterArray = isDerivativeCategoryFilterUsed ? derivativeCategoryFilter.split(",") : null;
        final String totalRowsQuery = setMediaByDomainIdQueryString(TOTAL_ROWS_QUERY, isActiveFilterUsed, isDerivativeCategoryFilterUsed,
                false, derivativeCategoryFilterArray);
        return jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(totalRowsQuery);
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
        }, (ResultSet resultSet) -> resultSet.next() ? Optional.of(resultSet.getInt(1)) : Optional.empty());
    }


    @Override
    public void addMedia(ImageMessage message) throws Exception {
        String domain = message.getOuterDomainData().getDomain().getDomain();
        String domainId = message.getOuterDomainData().getDomainId();
        String domainProvider = message.getOuterDomainData().getProvider();
        String domainField = WRITER.writeValueAsString(message.getOuterDomainData().getDomainFields());
        LOGGER.debug("insert media record with ImageMessageAvro sql={} MediaGuid={} RequestId={} ClientId={} Filename={} FileUrl={} Domain={}",
                ADD_WITH_IMAGEMESSAGEAVRO_QUERY,
                message.getMediaGuid(), message.getRequestId(), message.getClientId(), message.getFileName(), message.getFileUrl(), domain);
        jdbcTemplate.update((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(ADD_WITH_IMAGEMESSAGEAVRO_QUERY);
            statement.setString(1, message.getMediaGuid());
            statement.setString(2, message.getFileUrl());
            statement.setString(3, message.getFileName());
            statement.setInt(4, message.isActive() == null ? 0 : (message.isActive()) ? 1 : 0);
            statement.setString(5, message.getClientId());
            statement.setString(6, message.getUserId());
            statement.setInt(7, message.getHidden() == null ? 0 : (message.getHidden()) ? 1 : 0);
            statement.setString(8, App.MEDIA_SERVICE.getName());
            statement.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
            statement.setString(10, domain);
            statement.setString(11, domainId);
            statement.setString(12, domainProvider);
            statement.setString(13, domainField);
            statement.setString(14, message.getComment());
            statement.setString(15, message.getProvidedName());
            statement.setString(16, message.getCallback() == null ? "" : message.getCallback().toString());
            return statement;
        });
    }

    @Override
    public void updateMedia(ImageMessage message) throws Exception {
        String domain = message.getOuterDomainData().getDomain().getDomain();
        String domainId = message.getOuterDomainData().getDomainId();
        String domainProvider = message.getOuterDomainData().getProvider();
        String domainField = WRITER.writeValueAsString(message.getOuterDomainData().getDomainFields());
        LOGGER.debug("update media record with ImageMessageAvro sql={} MediaGuid={} RequestId={} ClientId={} Filename={} FileUrl={} Domain={}",
                UPDATE_WITH_IMAGEMESSAGEAVRO_QUERY,
                message.getMediaGuid(), message.getRequestId(), message.getClientId(), message.getFileName(), message.getFileUrl(), domain);
        jdbcTemplate.update((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(UPDATE_WITH_IMAGEMESSAGEAVRO_QUERY);
            statement.setInt(1, message.isActive() == null ? 0 : (message.isActive()) ? 1 : 0);
            statement.setString(2, message.getClientId());
            statement.setString(3, message.getUserId());
            statement.setInt(4, message.getHidden() == null ? 0 : (message.getHidden()) ? 1 : 0);
            statement.setString(5, App.MEDIA_SERVICE.getName());
            statement.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            statement.setString(7, domainField);
            statement.setString(8, message.getComment());
            statement.setString(9, domain);
            statement.setString(10, domainId);
            statement.setString(11, domainProvider);
            statement.setString(12, message.getFileUrl());
            statement.setString(13, message.getFileName());
            statement.setString(14, message.getProvidedName());
            statement.setString(15, message.getCallback() == null ? "" : message.getCallback().toString());
            statement.setString(16, message.getMediaGuid());
            return statement;
        });
    }

    @Override
    public List<Optional<Media>> getMediaByFilename(String fileName) {
        return jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(MEDIA_BY_FILE_NAME_QUERY);
            statement.setString(1, fileName);
            return statement;
        }, (ResultSet resultSet, int rowNumb) -> buildMediaFromResultSet(resultSet)).stream().collect(Collectors.toList());
    }

    @Override
    public Optional<Media> getMediaByGuid(String mediaGUID) {
        return jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(MEDIA_BY_GUID_QUERY);
            statement.setString(1, mediaGUID);
            return statement;
        }, (ResultSet resultSet) -> resultSet.next() ? buildMediaFromResultSet(resultSet) : Optional.empty());
    }


    @Override
    public List<Optional<Media>> getMediaByDomainId(String domainId) {
        return jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(HERO_MEDIA_QUERY);
            statement.setString(1, "Lodging");
            statement.setInt(2, Integer.valueOf(domainId));
            return statement;
        }, (ResultSet resultSet, int rowNumb) -> buildMediaFromResultSet(resultSet)).stream().collect(Collectors.toList());
    }

    @Override
    public void unheroMedia(String guid, String domainField) {
        jdbcTemplate.update((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(UPDATE_MEDIA_UNHERO_QUERY);
            statement.setString(1, domainField);
            statement.setString(2, domainField);
            return statement;
        });
    }

    @Override
    public List<Optional<Media>> getMediaByMediaId(String mediaId) {
        final String lcmMediaIdSubstring = "%\"lcmMediaId\":\"" + mediaId + "\"%";
        return jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(MEDIA_BY_LCM_MEDIA_ID);
            statement.setString(1, lcmMediaIdSubstring);
            return statement;
        }, (ResultSet resultSet, int rowNumb) -> buildMediaFromResultSet(resultSet)).stream().collect(Collectors.toList());
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

    @Override
    public List<MediaProcessLog> findMediaStatus(List<String> fileNames) {
        final String[] fileNamesArray = fileNames.toArray(new String[fileNames.size()]);
        List<MediaProcessLog> processLogList = new ArrayList<>();
        jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(setSQLTokensWithArray(MEDIAS_BY_FILE_NAMES_QUERY, fileNamesArray));
            setArray(statement, 1, fileNamesArray);
            return statement;
        }, (ResultSet resultSet, int rowNumb) -> processLogList.add(new MediaProcessLog(resultSet.getString("update-date"), resultSet.getString("file-name"),
                resultSet.getString("status"), resultSet.getString("domain"))));
        return processLogList;
    }


    @Override
    @SuppressWarnings("CPD-END")
    public void deleteMediaByGUID(String mediaGUID) {
        // no-op
    }

}
