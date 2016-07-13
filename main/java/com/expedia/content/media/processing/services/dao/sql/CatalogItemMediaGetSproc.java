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
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * This SProc returns attributes from CatalogItemMedia and DynamoMedia tables for the given
 * CatalogItem and MediaID
 * RESULT Set:
 * from CatalogItemMedia TABLE
 * 2) MediaID
 * 3) MediaUseRank
 * 8) UpdateDate
 * 9) LastUpdatedBy
 */
@Component
public class CatalogItemMediaGetSproc extends StoredProcedure {
    private static final String PROC_NAME = "CatalogItemMediaGet#01";
    private static final String PROC_RESULT = "result";

    @Autowired
    public CatalogItemMediaGetSproc(DataSource dataSource) {
        super(dataSource, PROC_NAME);
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlParameter("@pMediaID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(PROC_RESULT, new CatalogItemMediaRowMapper()));
    }

    private static class CatalogItemMediaRowMapper implements RowMapper<LcmCatalogItemMedia> {
        @Override
        public LcmCatalogItemMedia mapRow(ResultSet resultSet, int i) throws SQLException {
            Timestamp updateDate;
            try {
                updateDate = new Timestamp(TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("UpdateDate")).getTime());
            } catch (Exception e) {
                updateDate = null;
            }
            final LcmCatalogItemMedia media = LcmCatalogItemMedia.builder().catalogItemId(resultSet.getInt("CatalogItemID"))
                    .mediaId(resultSet.getInt("MediaId")).mediaUseRank(resultSet.getInt("MediaUseRank"))
                    .lastUpdateDate(updateDate)
                    .lastUpdatedBy(resultSet.getString("LastUpdatedBy")).build();
            return media;
        }
    }

    public List<LcmCatalogItemMedia> getMedia(int catalogItemId, int mediaId) {
        final Map<String, Object> results = execute(catalogItemId, mediaId);
        return (List<LcmCatalogItemMedia>) results.get(PROC_RESULT);
    }
}
