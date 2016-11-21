package com.expedia.content.media.processing.services.dao.sql;

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
 * Call a MSSQL Sproc [MediaSrchWithCatalogItemMediaAndMediaFileName#02] in LCM in order to
 * get LCM Media Id by Media File name and DomainId
 */
@Repository
public class GetMediaIDSproc extends StoredProcedure {

    public static final String MEDIA_SET = "media";

    @Autowired
    public GetMediaIDSproc(final DataSource dataSource) {
        super(dataSource, "MediaSrchWithCatalogItemMediaAndMediaFileName#03");
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlParameter("@pLangID", Types.INTEGER));
        declareParameter(new SqlParameter("@pContentProviderID", Types.INTEGER));
        declareParameter(new SqlParameter("@pContentProviderMediaName", Types.VARCHAR));
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
                    .mediaId(resultSet.getInt("MediaID"))
                    .lastUpdateDate(TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("UpdateDate")))
                    .lastUpdatedBy(resultSet.getString("LastUpdatedBy"))
                    .active(activeFlag != null && "A".equals(activeFlag))
                    .build();
        }
    }

}
