package com.expedia.content.media.processing.services.dao.sql;

import com.expedia.content.media.processing.services.dao.domain.LcmCatalogItemMedia;
import com.expedia.content.media.processing.services.util.TimeZoneWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * This SProc selects the list of Media Items for the given
 * CatalogItem, sorted by MediaUseRank
 */
@Component
public class MediaLstWithCatalogItemMediaAndMediaFileNameSproc extends StoredProcedure {
    private static final String PROC_NAME = "MediaLstWithCatalogItemMediaAndMediaFileName#03";
    private static final String PROC_RESULT = "result";

    @Autowired
    public MediaLstWithCatalogItemMediaAndMediaFileNameSproc(DataSource dataSource) {
        super(dataSource, PROC_NAME);
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(PROC_RESULT, new MediaLstRowMapper()));
    }

    private static class MediaLstRowMapper implements RowMapper<LcmCatalogItemMedia> {
        @Override
        public LcmCatalogItemMedia mapRow(ResultSet resultSet, int i) throws SQLException {
            final LcmCatalogItemMedia media = LcmCatalogItemMedia.builder()
                    .mediaId(resultSet.getInt("MediaId"))
                    .mediaUseRank(resultSet.getInt("MediaUseRank"))
                    .lastUpdateDate(TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("UpdateDate")))
                    .lastUpdatedBy(resultSet.getString("LastUpdatedBy"))
                    .build();
            return media;
        }
    }

    public List<LcmCatalogItemMedia> getMedia(int catalogItemId) {
        final Map<String, Object> results = execute(catalogItemId);
        return (List<LcmCatalogItemMedia>) results.get(PROC_RESULT);
    }
}
