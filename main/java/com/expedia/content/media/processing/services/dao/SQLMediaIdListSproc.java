package com.expedia.content.media.processing.services.dao;

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
public class SQLMediaIdListSproc extends StoredProcedure {

    public static final String MEDIA_ID_SET = "mediaIds";

    @Autowired
    public SQLMediaIdListSproc(final DataSource dataSource) {
        super(dataSource, "MediaLst#18");
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlParameter("@pLangID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(MEDIA_ID_SET, new MediaIdRowMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link Media}
     */
    private class MediaIdRowMapper implements RowMapper<Integer> {
        @Override
        public Integer mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            return resultSet.getInt("StatusCode");
        }
    }

}
