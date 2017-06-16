package com.expedia.content.media.processing.services.dao.mediadb;

import com.amazonaws.util.CollectionUtils;
import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.expedia.content.media.processing.services.util.MediaDBSQLUtil.setArray;
import static com.expedia.content.media.processing.services.util.MediaDBSQLUtil.setSQLTokensWithArray;

/**
 * Dao class for accessing Room Reference Data in MediaDB.
 */
public class MediaDBLodgingReferenceRoomIdDao {

    private static final String ROOM_LIST_QUERY = "SELECT `room-id` FROM `lodging-reference-room-id` WHERE `room-id` IN (?) AND `hotel-id` = ?";

    private final JdbcTemplate jdbcTemplate;

    public MediaDBLodgingReferenceRoomIdDao(DataSource mediaDBDataSource) {
        this.jdbcTemplate = new JdbcTemplate(mediaDBDataSource);
    }

    /**
     * Final all Invalid RoomIds in OuterDomain data.
     *
     * @param outerDomain The outerDomain to find roomIds in.
     * @return Returns a list of invalid roomIds, the list will be empty if no roomIds are invalid.
     * @throws ClassCastException
     */
    public List<Object> getInvalidRoomIds(OuterDomain outerDomain) throws ClassCastException {
        final List<String> validFormatRoomIds = DomainDataUtil.collectValidFormatRoomIds(outerDomain);
        final String[] arrayOfValidRoomIds = validFormatRoomIds.toArray(new String[validFormatRoomIds.size()]);
        if (outerDomain.getDomain().equals(Domain.LODGING) && !CollectionUtils.isNullOrEmpty(validFormatRoomIds)) {
            final String domainId = outerDomain.getDomainId();
            final String query = setSQLTokensWithArray(ROOM_LIST_QUERY, arrayOfValidRoomIds);
            final List<String> roomsInMediaDB = jdbcTemplate.query((Connection connection) -> {
                        final PreparedStatement statement = connection.prepareStatement(query);
                        int index = setArray(statement, 1, arrayOfValidRoomIds);
                        statement.setString(index, domainId);
                        return statement;
                    }, (ResultSet resultSet, int rowNumb) -> resultSet.getString("room-id")).stream().collect(Collectors.toList());
            validFormatRoomIds.removeAll(roomsInMediaDB);
        }
        final List<Object> malFormatRoomIds = DomainDataUtil.collectMalFormatRoomIds(outerDomain);
        malFormatRoomIds.addAll(validFormatRoomIds);
        return malFormatRoomIds;
    }
}
