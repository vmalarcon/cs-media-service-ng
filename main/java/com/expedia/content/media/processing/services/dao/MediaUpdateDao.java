package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;

public interface MediaUpdateDao {

    void updateMedia(ImageMessage imageMessage, int mediaId);

    LcmMedia getMediaByMediaId(int mediaId) ;
}
