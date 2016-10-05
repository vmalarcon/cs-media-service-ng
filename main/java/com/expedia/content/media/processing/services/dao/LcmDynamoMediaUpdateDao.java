package com.expedia.content.media.processing.services.dao;

import com.amazonaws.util.StringUtils;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.sql.MediaChgSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaGetSproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Media data access operations through LCM and the Dynamo MediaDB.
 */
@Component
public class LcmDynamoMediaUpdateDao implements MediaUpdateDao {
    private static final String UPDATED_BY = "Media Service";
    private static final FormattedLogger LOGGER = new FormattedLogger(LcmDynamoMediaUpdateDao.class);
    @Autowired
    private MediaChgSproc mediaChgSproc;
    @Autowired
    private SQLMediaGetSproc mediaGetByMediaIdSproc;

    public void updateMedia(ImageMessage imageMessage, int mediaId) {
        String statusCode = null;
        LOGGER.info("Update Media", imageMessage);
        if (imageMessage.isActive() != null) {
            statusCode = imageMessage.isActive() ? "A" : "I";
        }
        mediaChgSproc.updateMedia(mediaId,
                imageMessage.getComment(), statusCode,
                StringUtils.isNullOrEmpty(imageMessage.getUserId()) ? imageMessage.getClientId() : imageMessage.getUserId(),
                UPDATED_BY);
    }

    public void updateMediaTimestamp(LcmMedia lcmMedia, ImageMessage imageMessage) {
        final String statusCode = lcmMedia.getActive() ? "A" : "I";
        mediaChgSproc.updateMedia(lcmMedia.getMediaId(),
                lcmMedia.getComment(), statusCode,
                StringUtils.isNullOrEmpty(imageMessage.getUserId()) ? imageMessage.getClientId() : imageMessage.getUserId(),
                UPDATED_BY);
    }

    public LcmMedia getMediaByMediaId(int mediaId) {
        final Map<String, Object> mediaResult = mediaGetByMediaIdSproc.execute(mediaId);
        if (((List)mediaResult.get(SQLMediaGetSproc.MEDIA_SET)).isEmpty()) {
            return null;
        } else {
            return ((List<LcmMedia>) mediaResult.get(SQLMediaGetSproc.MEDIA_SET)).get(0);
        }
    }

}
