package com.expedia.content.media.processing.services.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Call a MSSQL Sproc [MediaProviderLst] in LCM in order to retrieve data from MediaProvider table
 */
@Repository
public class MediaProviderSproc extends StoredProcedure {

    public static final String MEDIA_PROVIDER_MAPPER_RESULT_SET = "MediaProvider";

    @Autowired
    public MediaProviderSproc(final DataSource dataSource) {
        super(dataSource, "MediaProviderLst");
        declareParameter(new SqlReturnResultSet(MEDIA_PROVIDER_MAPPER_RESULT_SET, new MediaProviderMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to an object
     * {@link MediaProvider}
     */
    class MediaProviderMapper implements RowMapper<MediaProvider> {

        @Override
        public MediaProvider mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final int mediaProviderID = resultSet.getInt("mediaProviderID");
            final String mediaProviderName = resultSet.getString("mediaProviderName");
            final Timestamp latitude = resultSet.getTimestamp("updateDate");
            final String longitude = resultSet.getString("lastUpdatedBy");
            final String updateLocation = resultSet.getString("updateLocation");
            return new MediaProvider(mediaProviderID, mediaProviderName, latitude, longitude, updateLocation);
        }
    }
}
