package com.expedia.content.media.processing.services.dao.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * when the room in update request does not exist in current db, we need to add to catalogItemMedian and paragraph if it is hero.
 * EXEC @RC = dbo.CatalogItemMediaAdd#02
 * 
 * @pCatalogItemID = @RoomID,
 * @pMediaID = @NewID,
 * @pMediaUseRank = @MediaSubcategoryID,
 * @pGalleryDisplayBool = 1,
 * @pMediaUseTypeID =1 ,
 * @pLastUpdatedBy = @pLastUpdatedBy,
 * @pPictureShowDisplayBool = 0,
 * @pParagraphDisplayBool = 1,
 * @pLastUpdateLocation ='DataManager';
 */
@Component
public class AddCatalogItemMediaForRoomsAndRatePlansSproc extends AddCatalogItemBaseSproc {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AddCatalogItemMediaForRoomsAndRatePlansSproc.class);

    private static final String PROC_NAME = "CatalogItemMediaAdd#02";
    
    @Autowired
    public AddCatalogItemMediaForRoomsAndRatePlansSproc(DataSource dataSource) {
        super(dataSource, PROC_NAME);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected String getProcName() {
        return PROC_NAME;
    }
}
