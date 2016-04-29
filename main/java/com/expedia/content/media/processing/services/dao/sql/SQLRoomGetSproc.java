package com.expedia.content.media.processing.services.dao.sql;


import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.object.StoredProcedure;

import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;

/**
 * Call a MSSQL Sproc in LCM in order to retrieve data from the Media,RoomTypeCatalogItemID, CatalogItemMedia, and Paragraph (derivatives) tables
 * to get current rooms of certain media.
 */
public abstract class SQLRoomGetSproc extends StoredProcedure {

    public static final String ROOM_SET = "room";
    
    public SQLRoomGetSproc(final DataSource dataSource, String sprocName) {
        super(dataSource, sprocName);
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link LcmMediaRoom}
     */
    protected class RoomRowMapper implements RowMapper<LcmMediaRoom> {
        @Override
        public LcmMediaRoom mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            return LcmMediaRoom.builder()
                    .mediaId(resultSet.getInt("MediaID"))
                    .roomId(resultSet.getInt("RoomTypeCatalogItemID"))
                    //IsAHeroMediaForRoom 1 means hero, 0 means not hero.
                    .roomHero(resultSet.getBoolean("IsAHeroMediaForRoom"))
                    .build();
        }
    }

}
