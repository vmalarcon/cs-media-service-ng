package com.expedia.content.media.processing.services.dao.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaDerivative;

/**
 * Call a MSSQL Sproc [MediaItemGet] in LCM in order to retrieve data from the Media, CatalogItemMedia, and MediaFileName (derivatives) tables
 */
@Repository
public class SQLMediaGetSproc extends StoredProcedure {

    public static final String MEDIA_SET = "media";
    public static final String MEDIA_DERIVATIVES_SET = "mediaDerivatives";

    @Autowired
    public SQLMediaGetSproc(final DataSource dataSource) {
        super(dataSource, "MediaItemGet#05");
        declareParameter(new SqlParameter("@pContentOwnerID", Types.INTEGER));
        declareParameter(new SqlParameter("@pMediaID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(MEDIA_SET, new MediaRowMapper()));
        declareParameter(new SqlReturnResultSet(MEDIA_DERIVATIVES_SET, new MediaDerivativeRowMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link LcmMedia}
     */
    private class MediaRowMapper implements RowMapper<LcmMedia> {
        @Override
        public LcmMedia mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final String activeFlag = resultSet.getString("StatusCode");
            return new LcmMedia(resultSet.getInt("CatalogItemID"), 
                    resultSet.getInt("MediaID"), 
                    resultSet.getString("ContentProviderMediaName"),
                    activeFlag != null && "A".equals(activeFlag) ? true : false,
                    resultSet.getInt("MediaWidth"),
                    resultSet.getInt("MediaHeight"),
                    resultSet.getInt("FileSizeKb"),
                    resultSet.getString("LastUpdatedBy"),
                    new Date(resultSet.getTimestamp("UpdateDate").getTime()),
                    resultSet.getInt("ContentProviderId"),
                    resultSet.getInt("MediaUseRank"),
                    resultSet.getString("MediaCommentTxt"));
        }
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link LcmMediaDerivative}
     */
    private class MediaDerivativeRowMapper implements RowMapper<LcmMediaDerivative> {
        @Override
        public LcmMediaDerivative mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final String processedFlag = resultSet.getString("FileProcessedBool");
            return new LcmMediaDerivative(resultSet.getInt("MediaID"),
                    resultSet.getInt("MediaSizeTypeID"), 
                    processedFlag != null && "1".equals(processedFlag) ? true : false, 
                    resultSet.getString("MediaFileName"), 
                    resultSet.getInt("MediaFileWidth"), 
                    resultSet.getInt("MediaFileHeight"), 
                    resultSet.getInt("FileSizeKb"));
        }
    }
}
