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
 * Call a MSSQL Sproc [CatalogItemMediaLst#01] in LCM in order to retrieve data from SKUGroupCatalogItem table
 */
@Repository
public class SKUGroupGetSproc extends StoredProcedure {

    public static final String SKU_GROUP_CATALOG_ITEM_MAPPER_RESULT_SET = "SKUGroupCatalogItem";

    @Autowired
    public SKUGroupGetSproc(final DataSource dataSource) {
        super(dataSource, "SKUGroupGet#16");
        declareParameter(new SqlParameter("@pSKUGroupCatalogItemID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(SKU_GROUP_CATALOG_ITEM_MAPPER_RESULT_SET, new SKUGroupCatalogItemMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to an object
     * {@link SKUGroupCatalogItem}
     */
    class SKUGroupCatalogItemMapper implements RowMapper<SKUGroupCatalogItem> {

        @Override
        public SKUGroupCatalogItem mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final String name = resultSet.getString("name");
            final int catalogItemStatusTypeID = resultSet.getInt("catalogItemStatusTypeID");
            final double latitude = resultSet.getDouble("latitude");
            final double longitude = resultSet.getDouble("longitude");
            final boolean fuzzyLatLongBool = resultSet.getBoolean("fuzzyLatLongBool");
            final int structureTypeID = resultSet.getInt("structureTypeID");
            final String airportCode = resultSet.getString("airportCode");
            final boolean chargeDepositBool = resultSet.getBoolean("chargeDepositBool");
            final String emailAddress = resultSet.getString("emailAddress");
            final int merchandisingTypeID = resultSet.getInt("merchandisingTypeID");
            final Timestamp catalogItemUpdateDate = new Timestamp(TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("catalogItem_UpdateDate")).getTime());
            final String catalogItemLastUpdatedBy = resultSet.getString("catalogItem_LastUpdatedBy");
            final Timestamp skuGroupUpdateDate = new Timestamp(TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("skuGroup_UpdateDate")).getTime());
            final String skuGroupLastUpdatedBy = resultSet.getString("skuGroup_LastUpdatedBy");
            final int priceLevelTypeID = resultSet.getInt("priceLevelTypeID");
            return new SKUGroupCatalogItem(name, catalogItemStatusTypeID, latitude, longitude, fuzzyLatLongBool, structureTypeID,
            airportCode, chargeDepositBool, emailAddress, merchandisingTypeID, catalogItemUpdateDate, catalogItemLastUpdatedBy,
                    skuGroupUpdateDate, skuGroupLastUpdatedBy, priceLevelTypeID);
        }
    }
}
