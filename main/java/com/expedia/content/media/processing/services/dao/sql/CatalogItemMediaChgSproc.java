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
 * Update Catalog Item Media table.
 *
 * dbo.CatalogItemMediaChg#03
 *(
 *      @pCatalogItemID               INT
 *    , @pMediaID                     INT
 *    , @pMediaUseRank                INT
 *    , @pGalleryDisplayBool          BIT
 *    , @pMediaUseTypeID              TINYINT
 *    , @pLastUpdatedBy               VARCHAR(32)
 *    , @pPictureShowDisplayBool      BIT
 *    , @pParagraphDisplayBool        BIT
 *    , @pLastUpdateLocation          VARCHAR(32)
 *)
 */
@Component
public class CatalogItemMediaChgSproc extends StoredProcedure {

    private static final FormattedLogger LOGGER = new FormattedLogger(CatalogItemMediaChgSproc.class);

    private static final String PROC_NAME = "CatalogItemMediaChg#03";

    @Autowired
    public CatalogItemMediaChgSproc(DataSource dataSource) {
        super(dataSource, PROC_NAME);

        declareParameter(new SqlParameter("@CatalogItemID", Types.INTEGER));
        declareParameter(new SqlParameter("@MediaID", Types.INTEGER));
        declareParameter(new SqlParameter("@MediaUseRank", Types.INTEGER));
        declareParameter(new SqlParameter("@GalleryDisplayBool", Types.BOOLEAN));
        declareParameter(new SqlParameter("@MediaUseTypeID", Types.TINYINT));
        declareParameter(new SqlParameter("@LastUpdatedBy", Types.VARCHAR));
        declareParameter(new SqlParameter("@PictureShowDisplayBool", Types.BOOLEAN));
        declareParameter(new SqlParameter("@ParagraphDisplayBool", Types.BOOLEAN));
        declareParameter(new SqlParameter("@LastUpdateLocation", Types.VARCHAR));
    }

    /**
     * Update the category for a catalog item media record.
     */
    public void updateCategory(int catalogItemId, int mediaId, int categoryId, String lastUpdateBy, String lastUpdateLocation) {
        LOGGER.info("Calling Sproc={} CatalogItemId={} MediaId={} CategoryId={} LastUpdateBy={} LastUpdateLocation={}",
                PROC_NAME, catalogItemId, mediaId, categoryId, lastUpdateBy, lastUpdateLocation);
        try {
            execute(catalogItemId, mediaId, categoryId, null, null, lastUpdateBy, null,  null, lastUpdateLocation);
        } catch (Exception e) {
            throw new MediaDBException("Error invoking: " + PROC_NAME, e);
        }
    }
}
