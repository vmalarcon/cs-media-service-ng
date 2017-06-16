package com.expedia.content.media.processing.services.dao.mediadb;

import com.expedia.content.media.processing.services.dao.DerivativesDao;
import com.expedia.content.media.processing.services.dao.domain.MediaDerivative;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class MediaDBDerivativesDao implements DerivativesDao {

    private static final String GET_DERIVATIVE_BY_MEDIA_GUID = "SELECT `media-id`, `location`, `type`, `width`, `height`, `file-size` FROM `derivative` " +
            "WHERE `media-id` = ?";
    private static final String GET_DERIVATIVE_BY_LOCATION = "SELECT `media-id`, `location`, `type`, `width`, `height`, `file-size` FROM `derivative` " +
            "WHERE `location` = ?";

    private final JdbcTemplate jdbcTemplate;

    public MediaDBDerivativesDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Optional<MediaDerivative> getDerivativeByMediaGuid(String mediaGuid) {
        return jdbcTemplate.query((Connection connection) -> {
            final PreparedStatement statement = connection.prepareStatement(GET_DERIVATIVE_BY_MEDIA_GUID);
            statement.setString(1, mediaGuid);
            return statement;
        }, (ResultSet resultSet) -> resultSet.next() ? Optional.of(MediaDerivative.builder()
                .mediaGuid(resultSet.getString("media-id"))
                .location(resultSet.getString("location"))
                .type(resultSet.getString("type"))
                .width(resultSet.getInt("width"))
                .fileSize(resultSet.getInt("file-size"))
                .build()) : Optional.empty());
    }

    @Override
    public Optional<MediaDerivative> getDerivativeByLocation(String location) {
        return jdbcTemplate.query((Connection connection) -> {
            final PreparedStatement statement = connection.prepareStatement(GET_DERIVATIVE_BY_LOCATION);
            statement.setString(1, location);
            return statement;
        }, (ResultSet resultSet) -> resultSet.next() ? Optional.of(MediaDerivative.builder()
                .mediaGuid(resultSet.getString("media-id"))
                .location(resultSet.getString("location"))
                .type(resultSet.getString("type"))
                .width(resultSet.getInt("width"))
                .fileSize(resultSet.getInt("file-size"))
                .build()) : Optional.empty());
    }
}
