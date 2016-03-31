package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.*;
import com.expedia.content.media.processing.services.dao.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Media data access operations through LCM and the Dynamo MediaDB.
 */
@Component
public class LcmDynamoMediaUpdateDao implements MediaUpdateDao {

    private static final int DEFAULT_LODGING_LOCALE = 1033;
    private static final int DEFAULT_CONTENT_PROVIDER_ID = 1;
    private static final String UPDATED_BY = "Media Service";

    private static final Logger LOGGER = LoggerFactory.getLogger(LcmDynamoMediaUpdateDao.class);

    @Autowired
    private MediaChgSproc mediaChgSproc;
    @Autowired
    private MediaGetByMediaIdSproc mediaTableGetSproc;

    @Autowired
    private MediaGetByMediaIdSproc mediaGetByMediaIdSproc;

    public void updateMedia(ImageMessage imageMessage, int mediaId) {
       // final LcmMedia lcmMedia = getMedia(mediaId);
        String statusCode = null;
        LOGGER.info("update media media[{}]" + mediaId);
        if(imageMessage.isActive()!=null) {
            statusCode = imageMessage.isActive() ? "A" : "I";
        }
        mediaChgSproc.updateMedia(mediaId,
                imageMessage.getComment(),  statusCode,
                 imageMessage.getUserId(),
                UPDATED_BY);
    }

    private LcmMedia getMedia(int mediaId) {
        final Map<String, Object> mediaResult = mediaTableGetSproc.execute(mediaId, DEFAULT_LODGING_LOCALE);
        return ((List<LcmMedia>) mediaResult.get(MediaTableGetSproc.MEDIA_SET)).get(0);
    }

    public LcmMedia getMediaByMediaId(int mediaId) {
        final Map<String, Object> mediaResult = mediaGetByMediaIdSproc.execute(mediaId);
        return ((List<LcmMedia>) mediaResult.get(MediaTableGetSproc.MEDIA_SET)).get(0);
    }

}
