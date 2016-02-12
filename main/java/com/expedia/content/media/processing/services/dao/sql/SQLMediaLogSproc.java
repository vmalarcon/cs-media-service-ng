package com.expedia.content.media.processing.services.dao.sql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Call a MSSQL Sproc [MediaProcessLogGetByFilename] in LCM in order to retrieve data from MediaProcessLog table
 */
@Repository
public class SQLMediaLogSproc extends StoredProcedure {

    public static final String MEDIAS_RESULT_SET = "medias";

    @Autowired
    public SQLMediaLogSproc(final DataSource dataSource) {
        super(dataSource, "MediaProcessLogGetByFilename");
        declareParameter(new SqlParameter("@pMediaFileNameList", Types.VARCHAR));
        declareParameter(new SqlReturnResultSet(MEDIAS_RESULT_SET, new MediaRowMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link MediaProcessLog}
     */
    private class MediaRowMapper implements RowMapper<MediaProcessLog> {
        @Override
        public MediaProcessLog mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final String mediaFileName = resultSet.getString("mediaFileName");
            final String activityNameAndType = resultSet.getString("ActivityType");
            final String activityTime = resultSet.getString("ActivityTime");
            final String mediaType = resultSet.getString("MediaType");
            return new MediaProcessLog(activityTime, mediaFileName, activityNameAndType, mediaType);
        }
    }
}
