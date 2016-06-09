package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.services.util.TimeZoneWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Call a MSSQL Sproc [PropertyRoomTypeGetID] in LCM in order to retrieve the rooms within a property
 */
@Repository
public class PropertyRoomTypeGetIDSproc extends StoredProcedure {

    public static final String ROOM_TYPE_RESULT_SET = "RoomType";

    @Autowired
    public PropertyRoomTypeGetIDSproc(final DataSource dataSource) {
        super(dataSource, "PropertyRoomTypeGetID");
        declareParameter(new SqlParameter("@pPropertyShellID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(ROOM_TYPE_RESULT_SET, new RoomTypeMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to an object
     * {@link RoomType}
     */
    class RoomTypeMapper implements RowMapper<RoomType> {

        @Override
        public RoomType mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            Timestamp updateDate;
            try {
                updateDate = new Timestamp(TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("updateDate")).getTime());
            } catch (Exception e)  {
                updateDate = null;
            }
            final int roomTypeCatalogItemID = resultSet.getInt("roomTypeContentID");
            final int roomTypeID = resultSet.getInt("roomTypeID");
            final String lastUpdatedBy = resultSet.getString("lastUpdatedBy");
            final String lastUpdateLocation = resultSet.getString("lastUpdateLocation");
            return new RoomType(roomTypeCatalogItemID, roomTypeID, updateDate, lastUpdatedBy, lastUpdateLocation);
        }
    }
}
