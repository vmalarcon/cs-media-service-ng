package com.expedia.content.media.processing.services.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import com.expedia.content.media.processing.services.util.TimeZoneWrapper;

/**
 * The RoomTypeThirdPartyAdd class is responsible for execute the stored procedure
 * in LCM to get all rooms from third party provider related to a property.
 */
@Repository
public class RoomTypeThirdPartyGet extends StoredProcedure {

    public static final String RESULTS_KEY = "RoomTypeThirdPartyGet";

    public RoomTypeThirdPartyGet(final DataSource dataSource) {
        super(dataSource, "dbo.RoomTypeThirdPartyGet");

        declareParameter(new SqlParameter("pSkuGroupCatalogItemId", Types.INTEGER));
        declareParameter(new SqlParameter("pTravelServiceProviderId", Types.TINYINT));
        declareParameter(new SqlParameter("pCatalogItemStatusTypeId", Types.INTEGER));
        declareParameter(new SqlParameter("pRoomTypeCatalogItemId", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(RESULTS_KEY, new RoomTypeMapper()));
        compile();
    }

    public Map<String, Object> getRooms(Integer propertyID, Integer travelServiceProviderID, Integer catalogItemStatusTypeId,
            Integer roomTypeCatalogItemId) {
        final Map<String, Object> inputs = new HashMap<>();
        inputs.put("pSkuGroupCatalogItemId", propertyID);
        inputs.put("pTravelServiceProviderId", travelServiceProviderID);
        inputs.put("pCatalogItemStatusTypeId", catalogItemStatusTypeId);
        inputs.put("pRoomTypeCatalogItemId", roomTypeCatalogItemId);
        return super.execute(inputs);
    }

    class RoomTypeMapper implements RowMapper<RoomType> {

        @Override
        public RoomType mapRow(ResultSet rs, int i) throws SQLException {
            Timestamp updateDate;
            try {
                updateDate = new Timestamp(TimeZoneWrapper.covertLcmTimeZone(rs.getString("updateDate")).getTime());
            } catch (Exception e) {
                updateDate = null;
            }
            final int roomTypeCatalogItemID = rs.getInt("roomTypeCatalogItemID");
            final String lastUpdatedBy = rs.getString("lastUpdatedBy");
            final String updateLocation = rs.getString("updateLocation");
            return new RoomType(roomTypeCatalogItemID, roomTypeCatalogItemID, updateDate, lastUpdatedBy, updateLocation);
        }
    }
}
