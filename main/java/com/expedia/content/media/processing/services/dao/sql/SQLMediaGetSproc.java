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

/**
 * Call a MSSQL Sproc [MediaGet] in LCM in order to retrieve data from the Media, CatalogItemMedia, and MediaFileName (derivatives) tables
 */
@Repository
public class SQLMediaGetSproc extends StoredProcedure {

    public static final String MEDIA_SET = "media";

    @Autowired
    public SQLMediaGetSproc(final DataSource dataSource) {
        super(dataSource, "MediaGet#19");
        declareParameter(new SqlParameter("@pMediaID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(MEDIA_SET, new MediaRowMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link LcmMedia}
     */
    private class MediaRowMapper implements RowMapper<LcmMedia> {
        @Override
        public LcmMedia mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final String activeFlag = resultSet.getString("StatusCode");
            return LcmMedia.builder()
                    .domainId(resultSet.getInt("SKUGroupCatalogItemID"))
                    .mediaId(resultSet.getInt("MediaID"))
                    .active(activeFlag != null && "A".equals(activeFlag))
                    .fileName(resultSet.getString("ContentProviderMediaName"))
                    .comment(resultSet.getString("MediaCommentTxt"))
                    .formatId(resultSet.getInt("MediaFormatID"))
                    .build();
        }
    }

}
