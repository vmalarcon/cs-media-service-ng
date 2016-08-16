package com.expedia.content.media.processing.services.dao.sql;


import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.dao.MediaDBException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.Arrays;

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
     *  @param catalogItemID
     * @param mediaId
     * @param mediaUseRank
     * @param galleryDisplay
     * @param mediaUserTypeId
     * @param lastUpdatedBy
     * @param pictureShowDisplay
     * @param paragraphDisplay
     * @param lastUpdateLocation
     * @param imageMessage
     */
    public void addCatalogItemMedia(int catalogItemID,
                                    int mediaId, int mediaUseRank,
                                    boolean galleryDisplay,
                                    int mediaUserTypeId,
                                    String lastUpdatedBy,
                                    boolean pictureShowDisplay,
                                    boolean paragraphDisplay,
                                    String lastUpdateLocation, ImageMessage imageMessage) {

        getLogger().info("Calling Sproc={} " +
                        "CatalogItemID={} " +
                        "MediaId={} " +
                        "MediauseRank={} " +
                        "GalleryDisplayBool={} " +
                        "MediaUserTypeId={} " +
                        "LastUpdatedBy={} " +
                        "PictureShowDisplayBool={} " +
                        "ParagraphDisplayBool={} " +
                        "LastUpdateLocation={} " +
                        "RequestId={} " +
                        "MediaGuid={}",
                getProcName(),
                catalogItemID,
                mediaId,
                mediaUseRank,
                galleryDisplay,
                mediaUserTypeId,
                lastUpdatedBy,
                pictureShowDisplay,
                paragraphDisplay,
                lastUpdateLocation,
                imageMessage.getRequestId(),
                imageMessage.getMediaGuid());

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
            getLogger().error(e, "Error invoking Sproc={}", Arrays.asList(getProcName()), imageMessage);
            throw new MediaDBException("Error executing: " + getProcName(), e);
        }
    }

    protected abstract FormattedLogger getLogger();
    protected abstract String getProcName();
}
