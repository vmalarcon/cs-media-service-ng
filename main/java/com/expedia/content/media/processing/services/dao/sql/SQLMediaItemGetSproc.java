package com.expedia.content.media.processing.services.dao.sql;

import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaDerivative;
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
import java.sql.Types;

/**
 * Call a MSSQL Sproc [MediaItemGet] in LCM in order to retrieve data from the Media, CatalogItemMedia, and MediaFileName (derivatives) tables
 */
@Repository
public class SQLMediaItemGetSproc extends StoredProcedure {

    public static final String MEDIA_SET = "media";
    public static final String MEDIA_DERIVATIVES_SET = "mediaDerivatives";

    @Autowired
    public SQLMediaItemGetSproc(final DataSource dataSource) {
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
    @SuppressWarnings("CPD-START")
    private class MediaRowMapper implements RowMapper<LcmMedia> {
        @Override
        public LcmMedia mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final String activeFlag = resultSet.getString("StatusCode");
            return LcmMedia.builder()
                    .domainId(resultSet.getInt("CatalogItemID"))
                    .mediaId(resultSet.getInt("MediaID"))
                    .provider(resultSet.getInt("MediaProviderID"))
                    .active(activeFlag != null && "A".equals(activeFlag))
                    .fileName(resultSet.getString("ContentProviderMediaName"))
                    .width(resultSet.getInt("MediaWidth"))
                    .height(resultSet.getInt("MediaHeight"))
                    .lastUpdatedBy(resultSet.getString("LastUpdatedBy"))
                    .fileSize(resultSet.getInt("FileSizeKb"))
                    .lastUpdateDate(TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("UpdateDate")))
                    .category(resultSet.getInt("MediaUseRank"))
                    .comment(resultSet.getString("MediaCommentTxt"))
                    .formatId(resultSet.getInt("MediaFormatID"))
                    .build();
        }
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link LcmMediaDerivative}
     */
    @SuppressWarnings("CPD-END")
    private class MediaDerivativeRowMapper implements RowMapper<LcmMediaDerivative> {
        @Override
        public LcmMediaDerivative mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            return LcmMediaDerivative.builder()
                    .fileName(resultSet.getString("MediaFileName"))
                    .mediaId(resultSet.getInt("MediaID"))
                    .mediaSizeTypeId(resultSet.getInt("MediaSizeTypeID"))
                    .fileProcessed(resultSet.getBoolean("FileProcessedBool"))
                    .width(resultSet.getInt("MediaFileWidth"))
                    .height(resultSet.getInt("MediaFileHeight"))
                    .fileSize(resultSet.getInt("FileSizeKb"))
                    .build();
        }
    }
}
