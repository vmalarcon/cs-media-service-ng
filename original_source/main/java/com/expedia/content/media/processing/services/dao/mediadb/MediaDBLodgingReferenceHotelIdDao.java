package com.expedia.content.media.processing.services.dao.mediadb;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dao class for accessing Hotel Reference Data in MediaDB.
 */
public class MediaDBLodgingReferenceHotelIdDao {
    private static final String HOTEL_ID_QUERY = "SELECT `name` FROM `lodging-reference-hotel-id` WHERE `hotel-id` = ?";

    private final JdbcTemplate jdbcTemplate;

    public MediaDBLodgingReferenceHotelIdDao(DataSource mediaDBDataSource) {
        this.jdbcTemplate = new JdbcTemplate(mediaDBDataSource);
    }

    /**
     * Verifies if a hotel-id (domainId) exists in the MediaDB.
     *
     * @param domainId The domainId to verify.
     * @return true if the domainId exists in the mediaDB, false otherwise.
     */
    public Boolean domainIdExists(String domainId) {
        final List<String> hotelList = jdbcTemplate.query((Connection connection) -> {
            final PreparedStatement statement = connection.prepareStatement(HOTEL_ID_QUERY);
            statement.setString(1, domainId);
            return statement;
        }, (ResultSet resultSet, int rowNumb) -> resultSet.getString("name")).stream().collect(Collectors.toList());
        return !(hotelList == null || hotelList.isEmpty());
    }
}
