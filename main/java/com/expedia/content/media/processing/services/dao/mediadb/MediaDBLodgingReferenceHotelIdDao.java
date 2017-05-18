package com.expedia.content.media.processing.services.dao.mediadb;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

public class MediaDBLodgingReferenceHotelIdDao {
    private static final String HOTEL_ID_QUERY = "SELECT `name` FROM `lodging-reference-hotel-id` WHERE `hotel-id` = ?";

    private final JdbcTemplate jdbcTemplate;

    public MediaDBLodgingReferenceHotelIdDao(DataSource mediaDBDataSource) {
        this.jdbcTemplate = new JdbcTemplate(mediaDBDataSource);
    }

    public Boolean domainIdExists(String domainId) {
        List<String> hotelList = jdbcTemplate.query((Connection connection) -> {
            PreparedStatement statement = connection.prepareStatement(HOTEL_ID_QUERY);
            statement.setString(1, domainId);
            return statement;
        }, (ResultSet resultSet, int rowNumb) -> resultSet.getString("name")).stream().collect(Collectors.toList());
        return !(hotelList == null || hotelList.isEmpty());
    }
}
