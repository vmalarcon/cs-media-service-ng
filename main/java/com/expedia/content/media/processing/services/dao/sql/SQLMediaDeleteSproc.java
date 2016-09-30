package com.expedia.content.media.processing.services.dao.sql;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.dao.MediaDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Types;

/**
 * Call a MSSQL Sproc [MediaDel] in LCM in order to retrieve data from the Media, CatalogItemMedia, and MediaFileName (derivatives) tables
 */
@Repository
public class SQLMediaDeleteSproc extends StoredProcedure {

    private static final FormattedLogger LOGGER = new FormattedLogger(SQLMediaDeleteSproc.class);
    private static final String PROC_NAME = "MediaDel";

    @Autowired
    public SQLMediaDeleteSproc(final DataSource dataSource) {
        super(dataSource, PROC_NAME);
        declareParameter(new SqlParameter("@pMediaID", Types.INTEGER));
    }

    /**
     * delete the media.
     */
    public void deleteMedia(int mediaId) {
        LOGGER.info("Calling Sproc={} MediaId={}", PROC_NAME, mediaId);
        try {
            execute(mediaId);
        } catch (Exception e) {
            LOGGER.error(e, "Error invoking Sproc={}", PROC_NAME);
            throw new MediaDBException("Error invoking: " + PROC_NAME, e);
        }
    }

}
