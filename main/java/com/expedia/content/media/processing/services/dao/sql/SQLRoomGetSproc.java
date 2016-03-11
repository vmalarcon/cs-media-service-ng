package com.expedia.content.media.processing.services.dao.sql;


import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Call a MSSQL Sproc [RoomTypeGetByMediaID] in LCM in order to retrieve data from the Media,RoomTypeCatalogItemID, CatalogItemMedia, and Paragraph (derivatives) tables
 */
@Repository
public class SQLRoomGetSproc extends StoredProcedure {

    public static final String MEDIA_SET = "room";

    @Autowired
    public SQLRoomGetSproc(final DataSource dataSource) {
        super(dataSource, "RoomTypeGetByMediaID");
        declareParameter(new SqlParameter("@pMediaID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(MEDIA_SET, new RoomRowMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link LcmMediaRoom}
     */
    private class RoomRowMapper implements RowMapper<LcmMediaRoom> {
        @Override
        public LcmMediaRoom mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            return LcmMediaRoom.builder()
                    .roomId(resultSet.getInt("RoomTypeCatalogItemID"))
                    .roomHero(resultSet.getBoolean("IsAHeroMediaForRoom"))
                    .build();
        }
    }

}
