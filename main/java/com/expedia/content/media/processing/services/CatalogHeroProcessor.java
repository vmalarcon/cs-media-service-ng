package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.dao.CatalogItemMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDBException;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.LcmCatalogItemMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaChgSproc;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.MediaLstWithCatalogItemMediaAndMediaFileNameSproc;
import com.expedia.content.media.processing.services.util.JSONUtil;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Date;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.expedia.content.media.processing.pipeline.domain.Domain.LODGING;

@Component
public class CatalogHeroProcessor {

    private static final FormattedLogger LOGGER = new FormattedLogger(CatalogHeroProcessor.class);
    private static final String SUBCATEGORY_ID = "subcategoryId";
    private static final int DEFAULT_USER_RANK = 0;
    private static final int HERO_USE_RANK = 3;
    private static final String ROOM_UPDATED_BY = "Media Service";
    private static final String LCM_PST_TIMEZONE = "America/Los_Angeles";
    private static final String DYNAMO_UTC_TIMEZONE = "UTC";
    @Autowired
    private DynamoMediaRepository mediaRepo;
    @Autowired
    private CatalogItemMediaChgSproc catalogItemMediaChgSproc;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private CatalogItemMediaDao catalogItemMediaDao;
    @Autowired
    CatalogItemMediaGetSproc catalogItemMediaGetSproc;
    @Autowired
    MediaLstWithCatalogItemMediaAndMediaFileNameSproc mediaLstWithCatalogItemMediaAndMediaFileNameSproc;

    /**
     * set the current media userank to hero 3, or set to subcategoryid from json
     *
     * @param user                userId from Json
     * @param lcmCatalogItemMedia catalogItemMedia from LCM DB
     * @param hero                if true, set hero to 3, or else mean unset hero
     * @param subCategoryId       subCategoryId from JSON
     */
    public void setMediaToHero(String user, LcmCatalogItemMedia lcmCatalogItemMedia, boolean hero, String subCategoryId) {
        if (hero) {
            catalogItemMediaChgSproc.updateCategory(lcmCatalogItemMedia.getCatalogItemId(), lcmCatalogItemMedia.getMediaId(),
                    HERO_USE_RANK, user, ROOM_UPDATED_BY);
        } else {
            if ("".equals(subCategoryId)) {
                catalogItemMediaChgSproc.updateCategory(lcmCatalogItemMedia.getCatalogItemId(), lcmCatalogItemMedia.getMediaId(),
                        DEFAULT_USER_RANK, user, ROOM_UPDATED_BY);
            } else {
                catalogItemMediaChgSproc.updateCategory(lcmCatalogItemMedia.getCatalogItemId(), lcmCatalogItemMedia.getMediaId(),
                        Integer.valueOf(subCategoryId), user, ROOM_UPDATED_BY);
            }
        }
    }

    /**
     * update lastUpdateBy and timestamp to catalogitemMedia table
     *
     * @param user
     * @param lcmCatalogItemMedia
     */
    public void updateTimestamp(String user, LcmCatalogItemMedia lcmCatalogItemMedia) {
        catalogItemMediaChgSproc.updateCategory(lcmCatalogItemMedia.getCatalogItemId(), lcmCatalogItemMedia.getMediaId(),
                lcmCatalogItemMedia.getMediaUseRank(), user, ROOM_UPDATED_BY);

    }

    /**
     * get the CatalogItemMedia from LCM DB.
     *
     * @param catalogItemId here the catalogItemId is domainID
     * @param mediaId
     * @return LcmCatalogItemMedia
     */
    public LcmCatalogItemMedia getCatalogItemMeida(int catalogItemId, int mediaId) {
        LcmCatalogItemMedia catalogItemMedia = null;
        final List<LcmCatalogItemMedia> catalogItemMedialist = catalogItemMediaGetSproc.getMedia(catalogItemId, mediaId);
        if (catalogItemMedialist != null && !catalogItemMedialist.isEmpty()) {
            catalogItemMedia =
                    catalogItemMedialist.get(0);
        }
        return catalogItemMedia;
    }

    /**
     * update the current media's useRank in catalogItemMedia table in LCM, if hero, set 3, if not hero, set the input subcategoryId.
     *
     * @param imageMessage json message
     * @param domainId
     * @param mediaId
     */
    public void updateCurrentMediaHero(ImageMessage imageMessage, int domainId, int mediaId) {
        catalogItemMediaDao.updateCatalogItem(imageMessage, Integer.valueOf(mediaId), domainId);
    }

    /**
     * Update category(MediaUserRank field) to old value found in Dynamo for all hero media belonging to the domain id
     * specified in the imageMessage parameter.
     *
     * @param imageMessage Image message to use
     */
    public void setOldCategoryForHeroPropertyMedia(ImageMessage imageMessage, String domainId, String guid, int mediaId) throws MediaDBException {
        try {
            final String catalogItemId = domainId;
            List<LcmCatalogItemMedia> lcmHeroMedia = mediaLstWithCatalogItemMediaAndMediaFileNameSproc.getMedia(Integer.parseInt(catalogItemId));
            lcmHeroMedia = lcmHeroMedia.stream()
                    .filter(item -> item.getMediaId() != mediaId)
                    .collect(Collectors.toList());
            List<Media> dynamoHeroMedia = mediaRepo.retrieveHeroPropertyMedia(catalogItemId, LODGING.getDomain());
            dynamoHeroMedia = dynamoHeroMedia.stream()
                    .filter(dynamoMedia -> !guid.equals(dynamoMedia.getMediaGuid()))
                    .filter(dynamoMedia -> StringUtils.isNotEmpty(dynamoMedia.getDomainFields()))
                    .collect(Collectors.toList());
            LOGGER.info("propertyHero count from dynamo DomainId={} Count={} Updating MediaGuid={}", domainId, dynamoHeroMedia.size(), guid);
            for (final Media dynamoMedia : dynamoHeroMedia) {
                    final Media mediaToSave = Media.of(dynamoMedia);
                    mediaToSave.setDomainFields(StringUtils.replace(dynamoMedia.getDomainFields(),
                            "\"propertyHero\":\"true\"", "\"propertyHero\":\"false\""));
                    mediaToSave.setUserId(imageMessage.getUserId());
                    mediaToSave.setLastUpdated(new Date());
                    LOGGER.info("Setting propertyHero false for DomainId={} MediaGuid={}", domainId, mediaToSave.getMediaGuid());
                    mediaDao.saveMedia(mediaToSave);
                    LOGGER.info("Completed setting propertyHero false for DomainId={} MediaGuid={}", domainId, mediaToSave.getMediaGuid());
            }
            final List<CategoryMedia> categoryMediaList = buildCategoryMediaList(lcmHeroMedia, dynamoHeroMedia);
            for (final CategoryMedia categoryMedia : categoryMediaList) {
                catalogItemMediaChgSproc.updateCategory(Integer.parseInt(catalogItemId), categoryMedia.getLcmMediaId(),
                        categoryMedia.getSubcategoryId(), imageMessage.getUserId(), ROOM_UPDATED_BY);
            }
        } catch (Exception ex) {
            final String error = String.format("ERROR updating previous category to hero image when processing GUID=[%s]",
                    guid);
            LOGGER.error(ex, error + " DomainID={}", domainId);
            throw new MediaDBException(error, ex);
        }
    }

    /**
     * Builds a list of CategoryMedia to be updated in LCM with the subcategoryId to be updated to.
     *
     * @param lcmMediaList    list of all LCM media of a property
     * @param dynamoMediaList list of Dynamo Media with propertyHero flag set to "true"
     * @return a list of CategoryMedia to be updated in LCM
     */
    private List<CategoryMedia> buildCategoryMediaList(List<LcmCatalogItemMedia> lcmMediaList, List<Media> dynamoMediaList) {
        final List<CategoryMedia> categoryMediaList = new ArrayList<>();
        final Map<Integer, Date> lcmIdLastUpdateDateMap = new HashMap<>();
        final Set<Integer> dynamoLcmIdSet = new HashSet<>();
        // Only add media that don't have mediaUseRank 3 and that have a corresponding PropertyHero Dynamo Media to the lcmIdLastUpdateDateMap.
        lcmMediaList.stream()
                .filter(media -> media.getMediaUseRank() != 3)
                .filter(media -> dynamoMediaList.stream()
                        .filter(dynamoMedia -> dynamoMedia.getLcmMediaId() != null)
                        .anyMatch(dynamoMedia -> Integer.parseInt(dynamoMedia.getLcmMediaId()) == media.getMediaId()))
                .forEach(media -> lcmIdLastUpdateDateMap.put(media.getMediaId(), media.getLastUpdateDate()));
        // Only get the subcategoryID from Dynamo if A dynamo media has an LCMMediaId and one of the following are true:
        // 1) A LCMMedia has a MediaUseRank 3
        // 2) The last update date in Dynamo is more recent than lCM
        dynamoMediaList.stream()
                .filter(media -> media.getLcmMediaId() != null && media.getDomainFields() != null)
                .filter(media -> (lcmIdLastUpdateDateMap.get(Integer.valueOf(media.getLcmMediaId())) == null) ? true :
                        compareDates(lcmIdLastUpdateDateMap.get(Integer.valueOf(media.getLcmMediaId())), media.getLastUpdated()))
                .forEach(media -> {
                    final Map map = JSONUtil.buildMapFromJson(media.getDomainFields());
                    final String subcategory = (String) map.get(SUBCATEGORY_ID);
                    final int subcategoryId = Integer.parseInt(StringUtils.isEmpty(subcategory) ? "0" : subcategory);
                    dynamoLcmIdSet.add(Integer.valueOf(media.getLcmMediaId()));
                    categoryMediaList.add(new CategoryMedia(subcategoryId, Integer.parseInt(media.getLcmMediaId())));
                });
        // If a media is not contained in the dynamoLcmIdSet and has a MediaUseRank of 3, give it a subcategoryId of 0.
        lcmMediaList.stream()
                .filter(media -> !dynamoLcmIdSet.contains(media.getMediaId()) && media.getMediaUseRank() == 3)
                .forEach(media -> categoryMediaList.add(new CategoryMedia(0, media.getMediaId())));
        return categoryMediaList;
    }

    /**
     * Compares a media's last update time in LCM and Dynamo DB.
     * LCM only holds the time to the minute, so the seconds are dropped from the Dynamo DB time to normalize the values.
     *
     * @param lcmDate    Last Update Time in LCM
     * @param dynamoDate Last Update Time in Dynamo DB
     * @return the boolean value of whether the Dynamo last update time is more recent than the LCM last update time
     */
    private Boolean compareDates(Date lcmDate, Date dynamoDate) {
        final ZonedDateTime lcmDateZoned = ZonedDateTime.ofInstant(lcmDate.toInstant(), ZoneId.of(LCM_PST_TIMEZONE));
        final ZonedDateTime dynamoDateZoned = ZonedDateTime.ofInstant(dynamoDate.toInstant(), ZoneId.of(DYNAMO_UTC_TIMEZONE));
        return !lcmDateZoned.isAfter(dynamoDateZoned.minusSeconds(dynamoDateZoned.getSecond()).minusNanos(dynamoDateZoned.getNano()));
    }

    private class CategoryMedia {
        private final Integer subcategoryId;
        private final Integer lcmMediaId;

        public CategoryMedia(Integer subcategoryId, Integer lcmMediaId) {
            this.subcategoryId = subcategoryId;
            this.lcmMediaId = lcmMediaId;
        }

        public Integer getSubcategoryId() {
            return this.subcategoryId;
        }

        public Integer getLcmMediaId() {
            return this.lcmMediaId;
        }
    }

}
