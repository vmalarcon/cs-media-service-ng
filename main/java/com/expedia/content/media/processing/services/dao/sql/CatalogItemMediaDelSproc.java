package com.expedia.content.media.processing.services.dao.sql;

import com.expedia.content.media.processing.services.dao.MediaDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Types;

/**
 * Delete one row Catalog Item Media table.
 * dbo.CatalogItemMediaChg#03
 * (
 *
 * @pCatalogItemID INT
 * )
 */
@Component
public class CatalogItemMediaDelSproc extends StoredProcedure {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogItemMediaDelSproc.class);

    private static final String PROC_NAME = "CatalogItemMediaDel";

    @Autowired
    public CatalogItemMediaDelSproc(DataSource dataSource) {
        super(dataSource, PROC_NAME);
        declareParameter(new SqlParameter("@pContentOwnerID", Types.INTEGER));
        declareParameter(new SqlParameter("@pMediaID", Types.INTEGER));

    }

    /**
     * Update the category for a catalog item media record.
     */
    public void deleteCategory(int cataLogItemId, int mediaId) {
        LOGGER.info("Calling: {} with:  mediaId=[{}]",
                PROC_NAME, mediaId);
        try {
            execute(cataLogItemId, mediaId);
        } catch (Exception e) {
            throw new MediaDBException("Error invoking: " + PROC_NAME, e);
        }
    }
}
