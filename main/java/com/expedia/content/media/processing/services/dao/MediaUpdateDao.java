package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;

public interface MediaUpdateDao {

    /**
     * update media table, here we only update 'statusCode', 'lastUpdatedBy','lastUpdatedLocation', 'comment'
     * @param imageMessage
     * @param mediaId
     */
    void updateMedia(ImageMessage imageMessage, int mediaId);

    /**
     *  update the update time and lastUpdated by in Media table
     * @param lcmMedia
     * @param imageMessage
     */
    void updateMediaTimestamp(LcmMedia lcmMedia, ImageMessage imageMessage);

    /**
     * get LCM media information by mediaID.
     * @param mediaId
     * @return
     */
    LcmMedia getMediaByMediaId(int mediaId) ;
}
