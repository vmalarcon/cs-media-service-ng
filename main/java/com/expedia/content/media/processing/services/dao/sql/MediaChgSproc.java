package com.expedia.content.media.processing.services.dao.sql;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.dao.MediaDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Types;

/**
 * Update  Media table.
 * [dbo].[MediaSet#02] (
 *
 * @pMediaID INT
 * , @pLangID                      INT = 1033
 * , @pMediaFormatID               INT
 * , @pContentProviderID           INT
 * , @pMediaCreditTxt              VARCHAR(255)
 * , @pMediaCommentTxt             VARCHAR(255)
 * , @pContentProviderMediaName    VARCHAR(255)
 * , @pStatusCode                  CHAR(1)
 * , @pMediaStartHorizontalPct     DECIMAL(7,6)
 * , @pMediaDisplayMethodSeqNbr    TINYINT = NULL
 * , @pMediaDisplayName            NVARCHAR(32)
 * , @pMediaCaptionTxt             NVARCHAR(255)
 * , @pLastUpdatedBy               VARCHAR(32)
 * , @pLastUpdateLocation          VARCHAR(32)
 * )
 */
@Component
public class MediaChgSproc extends StoredProcedure {

    private static final FormattedLogger LOGGER = new FormattedLogger(MediaChgSproc.class);

    private static final String PROC_NAME = "MediaSet#03";

    @Autowired
    public MediaChgSproc(DataSource dataSource) {
        super(dataSource, PROC_NAME);
        declareParameter(new SqlParameter("@pMediaID", Types.INTEGER));
        declareParameter(new SqlParameter("@pLangID", Types.INTEGER));
        declareParameter(new SqlParameter("@pMediaFormatID", Types.INTEGER));
        declareParameter(new SqlParameter("@pContentProviderID", Types.INTEGER));
        declareParameter(new SqlParameter("@pMediaCreditTxt", Types.VARCHAR));
        declareParameter(new SqlParameter("@pMediaCommentTxt", Types.VARCHAR));
        declareParameter(new SqlParameter("@pContentProviderMediaName", Types.VARCHAR));
        declareParameter(new SqlParameter("@pStatusCode", Types.CHAR));
        declareParameter(new SqlParameter("@pMediaStartHorizontalPct", Types.DECIMAL));
        declareParameter(new SqlParameter("@pMediaDisplayMethodSeqNbr", Types.TINYINT));
        declareParameter(new SqlParameter("@pMediaDisplayName", Types.VARCHAR));
        declareParameter(new SqlParameter("@pMediaCaptionTxt", Types.VARCHAR));
        declareParameter(new SqlParameter("@MediaProviderID", Types.INTEGER));
        declareParameter(new SqlParameter("@pLastUpdatedBy", Types.VARCHAR));
        declareParameter(new SqlParameter("@pLastUpdateLocation", Types.VARCHAR));

    }

    /**
     * Update LCM media table.
     */

    public void updateMedia(int mediaId, String mediaCommentTxt,
            String statusCode, String lastUpdateBy, String lastUpdateLocation) {
        LOGGER.info("Calling Sproc={} MediaId={} MediaCommentTxt={} "
                        + "StatusCode={} LastUpdateBy={} LastUpdateLocation={}",
                PROC_NAME, mediaId, mediaCommentTxt, statusCode,
                lastUpdateBy, lastUpdateLocation);
        try {

            execute(mediaId, null, null, null, null, mediaCommentTxt, null, statusCode,
                    null, null, null, null, null, lastUpdateBy, lastUpdateLocation);
        } catch (Exception e) {
            throw new MediaDBException("Error invoking: " + PROC_NAME, e);
        }
    }
}
