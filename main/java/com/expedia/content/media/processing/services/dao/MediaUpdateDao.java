package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;

import java.util.List;

public interface MediaUpdateDao {



    public void updateMedia(ImageMessage imageMessage, int mediaId);
}
