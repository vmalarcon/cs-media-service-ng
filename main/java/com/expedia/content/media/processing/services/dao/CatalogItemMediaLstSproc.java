package com.expedia.content.media.processing.services.dao;

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
 * Call a MSSQL Sproc [CatalogItemMediaLst#01] in LCM in order to retrieve data from CatalogItemMedia table
 */
@Repository
public class CatalogItemMediaLstSproc extends StoredProcedure {

    public static final String CatalogItemMedia_RESULT_SET = "CatalogItemMedias";

    @Autowired
    public CatalogItemMediaLstSproc(final DataSource dataSource) {
        super(dataSource, "CatalogItemMediaLst#01");
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(CatalogItemMedia_RESULT_SET, new CatalogItemMediaMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to an object
     * {@link CatalogItemMedia}
     */
    class CatalogItemMediaMapper implements RowMapper<CatalogItemMedia> {

        @Override
        public CatalogItemMedia mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final int catalogItemId = resultSet.getInt("catalogItemId");
            final int mediaID = resultSet.getInt("mediaID");
            final int mediaUseRank = resultSet.getInt("mediaUseRank");
            final boolean galleryDisplayBool = resultSet.getBoolean("galleryDisplayBool");
            final int mediaUseTypeID = resultSet.getInt("mediaUseTypeID");
            final int updateTravelProductID = resultSet.getInt("updateTravelProductID");
            final int updateTUID = resultSet.getInt("updateTUID");
            final boolean pictureShowDisplayBool = resultSet.getBoolean("pictureShowDisplayBool");
            final boolean paragraphDisplayBool = resultSet.getBoolean("paragraphDisplayBool");
            final String lastUpdatedBy = resultSet.getString("lastUpdatedBy");
            final String updateLocation = resultSet.getString("updateLocation");
            return new CatalogItemMedia(catalogItemId, mediaID, mediaUseRank, galleryDisplayBool, mediaUseTypeID, updateTravelProductID, updateTUID,
                    pictureShowDisplayBool, paragraphDisplayBool, lastUpdatedBy, updateLocation);
        }
    }
}
