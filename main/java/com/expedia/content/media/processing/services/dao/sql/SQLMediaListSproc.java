package com.expedia.content.media.processing.services.dao.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaAndDerivative;
import com.expedia.content.media.processing.services.util.TimeZoneWrapper;

/**
 * Call a MSSQL Sproc [MediaItemListGet] in LCM in order to retrieve data from the Media, CatalogItemMedia, and MediaFileName (derivatives) tables
 */
@Repository
public class SQLMediaListSproc extends StoredProcedure {

    public static final String MEDIA_SET = "media";

    @Autowired
    public SQLMediaListSproc(final DataSource dataSource) {
        super(dataSource, "MediaFileNameLstByCatalogItemID");
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(MEDIA_SET, new MediaIdRowMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link LcmMedia}
     */
    private class MediaIdRowMapper implements RowMapper<LcmMediaAndDerivative> {
        @Override
        public LcmMediaAndDerivative mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final String activeFlag = resultSet.getString("StatusCode");
            return LcmMediaAndDerivative.builder()
                    .mediaId(resultSet.getInt("MediaID"))
                    .provider(resultSet.getInt("MediaProviderID"))
                    .active(activeFlag != null && "A".equals(activeFlag))
                    .fileName(resultSet.getString("ContentProviderMediaName"))
                    .width(resultSet.getInt("MediaWidth"))
                    .height(resultSet.getInt("MediaHeight"))
                    .lastUpdatedBy(resultSet.getString("LastUpdatedBy"))
                    .fileSize(resultSet.getInt("FileSizeKb"))
                    .lastUpdateDate(TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("UpdateDate")))
                    .mediaLastUpdatedBy(resultSet.getString("MediaLastUpdatedBy"))
                    .mediaLastUpdateDate(TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("MediaUpdateDate")))
                    .category(resultSet.getInt("MediaUseRank"))
                    .comment(resultSet.getString("MediaCommentTxt"))
                    .formatId(resultSet.getInt("MediaFormatID"))
                    .derivativeFileName(resultSet.getString("MediaFileName"))
                    .derivativeSizeTypeId(resultSet.getInt("MediaSizeTypeID"))
                    .fileProcessed(resultSet.getBoolean("FileProcessedBool"))
                    .derivativeWidth(resultSet.getInt("MediaFileWidth"))
                    .derivativeHeight(resultSet.getInt("MediaFileHeight"))
                    .derivativeFileSize(resultSet.getInt("DerivativeFileSizeKb"))
                    .build();
        }
    }

}
