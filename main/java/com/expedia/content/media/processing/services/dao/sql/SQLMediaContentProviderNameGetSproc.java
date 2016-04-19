package com.expedia.content.media.processing.services.dao.sql;

import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
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
 * Call a MSSQL Sproc [MediaSrchWithDerivativeFileName] in LCM in order to retrieve data from the Media, CatalogItemMedia, and MediaFileName (derivatives) tables
 */
@Repository
public class SQLMediaContentProviderNameGetSproc extends StoredProcedure {

    public static final String MEDIA_ATTRS = "mediaAttrs";

    @Autowired
    public SQLMediaContentProviderNameGetSproc(final DataSource dataSource) {
        super(dataSource, "MediaSrchWithDerivativeFileName");
        declareParameter(new SqlParameter("@pMediaFileName", Types.VARCHAR));
        declareParameter(new SqlReturnResultSet(MEDIA_ATTRS, new MediaIdRowMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link LcmMedia}
     */
    private class MediaIdRowMapper implements RowMapper<LcmMedia> {
        @Override
        public LcmMedia mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final String activeFlag = resultSet.getString("StatusCode");
            return LcmMedia.builder()
                    .fileName(resultSet.getString("ContentProviderMediaName"))
                    .domainId(resultSet.getInt("SKUGroupCatalogItemID"))
                    .filProcessedBool(activeFlag != null && "A".equals(activeFlag) ? true : false)
                    .mediaId(resultSet.getInt("MediaID"))
                    .build();
        }
    }

}
