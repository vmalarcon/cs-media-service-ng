package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.*;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.*;
import com.expedia.content.media.processing.services.util.ActivityMapping;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private MediaTableGetSproc mediaTableGetSproc;

    @Autowired
    private DynamoMediaRepository mediaRepo;
    @Autowired
    private LcmProcessLogDao processLogDao;
    @Autowired
    private List<ActivityMapping> activityWhiteList;
    @Resource(name = "providerProperties")
    private Properties providerProperties;
    @Value("${image.root.path}")
    private String imageRootPath;
    @Value("${media.status.sproc.param.limit}")
    private int paramLimit;

    public void updateMedia(ImageMessage imageMessage, int mediaId) {
        LcmMedia lcmMedia = getMedia(mediaId);
        LOGGER.info("update media media[{}]" + mediaId);
        final String statusCode = lcmMedia.getActive() ? "A" : "I";
        mediaChgSproc.updateMedia(mediaId, DEFAULT_LODGING_LOCALE, lcmMedia.getFormatId(), DEFAULT_CONTENT_PROVIDER_ID, lcmMedia.getMediaCreditTxt(),
                imageMessage.getComment(), lcmMedia.getFileName(), statusCode, lcmMedia.getMediaStartHorizontalPct(),
                lcmMedia.getMediaDisplayMethodSeqNbr() == 0 ? null : lcmMedia.getMediaDisplayMethodSeqNbr(), lcmMedia.getMediaCaptionTxt(),
                lcmMedia.getMediaDisplayName(), imageMessage.getUserId(),
                UPDATED_BY);
    }

    private LcmMedia getMedia(int mediaId) {
        final Map<String, Object> mediaResult = mediaTableGetSproc.execute(mediaId, DEFAULT_LODGING_LOCALE);
        final LcmMedia media = ((List<LcmMedia>) mediaResult.get(MediaTableGetSproc.MEDIA_SET)).get(0);
        return media;
    }

}
