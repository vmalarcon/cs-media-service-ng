package com.expedia.content.media.processing.services.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaDerivative;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaIdListSproc;
import com.expedia.content.media.processing.services.util.ActivityMapping;
import com.expedia.content.media.processing.services.util.JSONUtil;

/**
 * TODO
 */
@Component
public class MediaDao {

    private static final int DEFAULT_LODGING_LOCALE = 1033;
    private static final String ACTIVE_FILTER_ALL = "all";
    private static final String ACTIVE_FILTER_TRUE = "true";
    private static final String ACTIVE_FILTER_FALSE = "false";
    @SuppressWarnings("serial")
    private static final Map<Integer, String> TYPE_MAP = Collections.unmodifiableMap(new HashMap<Integer, String>() {
        {
            put(1, "t");
            put(2, "s");
            put(3, "b");
            put(4, "c");
            put(5, "x");
            put(6, "f");
            put(7, "p");
            put(8, "v");
            put(9, "l");
            put(10, "m");
            put(11, "n");
            put(12, "g");
            put(13, "d");
            put(14, "y");
            put(15, "z");
            put(16, "e");
            put(17, "w");
        }
    });

    private final SQLMediaIdListSproc lcmMediaIdSproc;
    private final SQLMediaGetSproc lcmMediaSproc;
    private final DynamoMediaRepository mediaRepo;
    private final LcmProcessLogDao processLogDao;

    @Autowired
    private List<ActivityMapping> activityWhiteList;
    @Resource(name = "providerProperties")
    private Properties providerProperties;

    @Autowired
    public MediaDao(SQLMediaIdListSproc lcmMediaIdSproc, SQLMediaGetSproc lcmMediaSproc, DynamoMediaRepository mediaRepo, LcmProcessLogDao processLogDao) {
        this.lcmMediaIdSproc = lcmMediaIdSproc;
        this.lcmMediaSproc = lcmMediaSproc;
        this.mediaRepo = mediaRepo;
        this.processLogDao = processLogDao;
    }

    /**
     * TODO
     * 
     * @param domain
     * @param domainId
     * @param activeFilter
     * @param derivativeFilter
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Media> getMediaByDomainId(String domain, String domainId, String activeFilter, String derivativeFilter) {
        List<Media> domainIdMedia =
                mediaRepo.loadMedia(domainId).stream().filter(media -> media.getDomain().equalsIgnoreCase(domain)).collect(Collectors.toList());
        if ("lodging".equals(domain.toLowerCase(Locale.US))) {
            final Map<String, Media> mediaEidMap = domainIdMedia.stream().collect(Collectors.toMap(Media::getLcmMediaId, media -> media));
            final Map<String, Object> idResult = lcmMediaIdSproc.execute(Integer.parseInt(domainId), DEFAULT_LODGING_LOCALE);
            final List<Integer> mediaIds = (List<Integer>) idResult.get(SQLMediaIdListSproc.MEDIA_ID_SET);
            /* @formatter:off */
            final List<Media> lcmMediaList = mediaIds.stream()
                    .map(buildLcmMedia(domainId, derivativeFilter))
                    .map(buildMedia(mediaEidMap))
                    .collect(Collectors.toList());
            /* @formatter:on */
            domainIdMedia.removeAll(mediaEidMap.values());
            domainIdMedia.addAll(0, lcmMediaList);
        }
        domainIdMedia = domainIdMedia.stream()
                .filter(media -> (activeFilter == null || activeFilter.isEmpty() || activeFilter.equals(ACTIVE_FILTER_ALL)
                        || (activeFilter.equals(ACTIVE_FILTER_TRUE) && media.getActive().equals(ACTIVE_FILTER_TRUE))
                        || (activeFilter.equals(ACTIVE_FILTER_FALSE) && !media.getActive().equals(ACTIVE_FILTER_FALSE))) ? true : false)
                .collect(Collectors.toList());
        return domainIdMedia;
    }

    /**
     * @param domainId
     * @param derivativeFilter
     * @return
     */
    @SuppressWarnings("unchecked")
    private Function<Integer, LcmMedia> buildLcmMedia(String domainId, String derivativeFilter) {
        /* @formatter:off */
        return mediaId -> {
            final Map<String, Object> mediaResult = lcmMediaSproc.execute(Integer.parseInt(domainId), mediaId);
            final LcmMedia media = ((List<LcmMedia>) mediaResult.get(SQLMediaGetSproc.MEDIA_SET)).get(0);
            media.setDerivatives(((List<LcmMediaDerivative>) mediaResult.get(SQLMediaGetSproc.MEDIA_DERIVATIVES_SET)).stream()
                    .filter(derivative -> (derivativeFilter == null || derivativeFilter.isEmpty() ||
                                           derivativeFilter.contains(derivative.getMediSizeType())) ? true : false)
                    .collect(Collectors.toList()));
            return media;
        };
        /* @formatter:on */
    }

    /**
     * TODO
     * 
     * @param mediaEidMap
     * @return
     */
    private Function<LcmMedia, Media> buildMedia(final Map<String, Media> mediaEidMap) {
        return lcmMedia -> {
            final Media dynamoMedia = mediaEidMap.get(lcmMedia.getMediaId().toString());
            final Media newMedia = new Media();
            newMedia.setActive(lcmMedia.isActive().toString());
            newMedia.setCallback((dynamoMedia == null) ? null : dynamoMedia.getCallback());
            newMedia.setClientId((dynamoMedia == null) ? null : dynamoMedia.getClientId());
            newMedia.setDerivatives(extractDerivatives(lcmMedia));
            newMedia.setDomain((dynamoMedia == null) ? null : dynamoMedia.getDomain());
            newMedia.setDomainFields(extractDomainFields(lcmMedia, dynamoMedia));
            newMedia.setDomainId((dynamoMedia == null) ? null : dynamoMedia.getDomainId());
            newMedia.setFileName(lcmMedia.getFileName());
            newMedia.setLastUpdated(lcmMedia.getLastUpdateDate());
            newMedia.setLcmMediaId(lcmMedia.getMediaId().toString());
            newMedia.setMediaGuid((dynamoMedia == null) ? null : dynamoMedia.getMediaGuid());
            newMedia.setProvider((dynamoMedia == null)
                                                       ? (lcmMedia.getProvider() == null ? null
                                                                                         : providerProperties
                                                                                                 .getProperty(lcmMedia.getProvider().toString()))
                                                       : dynamoMedia.getProvider());
            newMedia.setSourceUrl((dynamoMedia == null) ? null : dynamoMedia.getSourceUrl());
            newMedia.setUserId(lcmMedia.getLastUpdatedBy());
            newMedia.setStatus(getLatestStatus(lcmMedia));
            return newMedia;
        };
    }

    /**
     * TODO
     * 
     * @param lcmMedia
     * @return
     */
    @SuppressWarnings("serial")
    private String getLatestStatus(LcmMedia lcmMedia) {
        final List<MediaProcessLog> logs = processLogDao.findMediaStatus(new ArrayList<String>() {
            {
                add(lcmMedia.getFileName());
            }
        });
        final ActivityMapping activityStatus =
                logs.isEmpty() ? null : JSONUtil.getMappingFromList(activityWhiteList, logs.get(0).getActivityType(), logs.get(0).getMediaType());
        return activityStatus == null ? null : activityStatus.getStatusMessage();
    }

    private String extractDomainFields(LcmMedia lcmMedia, Media dynamoMedia) {
        // TODO Auto-generated method stub
        return null;
    }

    private String extractDerivatives(LcmMedia lcmMedia) {
        // TODO Auto-generated method stub
        return null;
    }

}
