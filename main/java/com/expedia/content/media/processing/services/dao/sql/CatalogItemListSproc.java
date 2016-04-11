package com.expedia.content.media.processing.services.dao.sql;

import com.expedia.content.media.processing.services.dao.domain.LcmCatalogItemMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
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
public class CatalogItemListSproc extends StoredProcedure {

    public static final String MEDIA_SET = "media";

    @Autowired
    public CatalogItemListSproc(final DataSource dataSource) {
        super(dataSource, "CatalogItemMediaLst#01");
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(MEDIA_SET, new MediaRowMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link LcmMedia}
     */
    private class MediaRowMapper implements RowMapper<LcmCatalogItemMedia> {
        @Override
        public LcmCatalogItemMedia mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            return LcmCatalogItemMedia.builder()
                    .catalogItemId(resultSet.getInt("CatalogItemID"))
                    .mediaId(resultSet.getInt("MediaID"))
                    .fileName(resultSet.getString("ContentProviderMediaName"))
                    .comment(resultSet.getString("MediaCommentTxt"))
                    .mediaUseRank(resultSet.getInt("MediaUseRank"))
                    .lastUpdateDate(TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("UpdateDate")))
                    .build();
        }
    }


}
