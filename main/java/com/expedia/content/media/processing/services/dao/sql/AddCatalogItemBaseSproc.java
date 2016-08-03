package com.expedia.content.media.processing.services.dao.sql;

import com.expedia.content.media.processing.pipeline.reporting.FormattedLogger;
import com.expedia.content.media.processing.services.dao.MediaDBException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;

import javax.sql.DataSource;
import java.sql.Types;

/**
 * Base for both stored procedures:
 *
 * @see AddCatalogItemMediaForRoomsAndRatePlansSproc
 */
public abstract class AddCatalogItemBaseSproc extends StoredProcedure {

    public AddCatalogItemBaseSproc(DataSource ds, String name) {
        super(ds, name);
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlParameter("@pMediaID", Types.INTEGER));
        declareParameter(new SqlParameter("@pMediaUseRank", Types.INTEGER));
        declareParameter(new SqlParameter("@pGalleryDisplayBool", Types.BOOLEAN));
        declareParameter(new SqlParameter("@pMediaUseTypeID", Types.TINYINT));
        declareParameter(new SqlParameter("@pLastUpdatedBy", Types.VARCHAR));
        declareParameter(new SqlParameter("@pPictureShowDisplayBool", Types.BOOLEAN));
        declareParameter(new SqlParameter("@pParagraphDisplayBool", Types.BOOLEAN));
        declareParameter(new SqlParameter("@pLastUpdateLocation", Types.VARCHAR));
    }

    /**
     * Execute the CatalogItemMediaAdd#02 Stored procedure in order to populate the CatalogItemMedia table.
     *
     * @param catalogItemID
     * @param mediaId
     * @param mediaUseRank
     * @param galleryDisplay
     * @param mediaUserTypeId
     * @param lastUpdatedBy
     * @param pictureShowDisplay
     * @param paragraphDisplay
     * @param lastUpdateLocation
     */
    public void addCatalogItemMedia(int catalogItemID,
                                    int mediaId, int mediaUseRank,
                                    boolean galleryDisplay,
                                    int mediaUserTypeId,
                                    String lastUpdatedBy,
                                    boolean pictureShowDisplay,
                                    boolean paragraphDisplay,
                                    String lastUpdateLocation) {

        getLogger().info("Calling {} " +
                        "CatalogItemID={} " +
                        "MediaId={} " +
                        "MediauseRank={} " +
                        "GalleryDisplayBool={} " +
                        "MediaUserTypeId={} " +
                        "LastUpdatedBy={} " +
                        "PictureShowDisplayBool={} " +
                        "ParagraphDisplayBool={} " +
                        "LastUpdateLocation={}",
                getProcName(),
                catalogItemID,
                mediaId,
                mediaUseRank,
                galleryDisplay,
                mediaUserTypeId,
                lastUpdatedBy,
                pictureShowDisplay,
                paragraphDisplay,
                lastUpdateLocation);

        try {
            execute(catalogItemID,
                    mediaId,
                    mediaUseRank,
                    galleryDisplay,
                    mediaUserTypeId,
                    lastUpdatedBy,
                    pictureShowDisplay,
                    paragraphDisplay,
                    lastUpdateLocation);
        } catch (Exception e) {
            getLogger().error(e, "Error invoking Sproc={}", getProcName());
            throw new MediaDBException("Error executing: " + getProcName(), e);
        }
    }

    protected abstract FormattedLogger getLogger();
    protected abstract String getProcName();
}
