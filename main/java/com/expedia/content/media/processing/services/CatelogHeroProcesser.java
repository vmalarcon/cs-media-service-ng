package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.CatalogitemMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDBException;
import com.expedia.content.media.processing.services.dao.domain.LcmCatalogItemMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemListSproc;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaChgSproc;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.MediaLstWithCatalogItemMediaAndMediaFileNameSproc;
import com.expedia.content.media.processing.services.util.JSONUtil;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.expedia.content.media.processing.pipeline.domain.Domain.LODGING;

@Component
public class CatelogHeroProcesser {

    private static final String SUBCATEGORY_ID = "subcategoryId";
    private static final int DEFAULT_USER_RANK = 0;
    private static final String ROOM_UPDATED_BY = "Media Service";
    @Autowired
    private DynamoMediaRepository mediaRepo;
    @Autowired
    private CatalogItemMediaChgSproc catalogItemMediaChgSproc;
    @Autowired
    private CatalogItemListSproc catalogItemListSproc;
    @Autowired
    private CatalogitemMediaDao catalogitemMediaDao;
    @Autowired
    CatalogItemMediaGetSproc catalogItemMediaGetSproc;
    @Autowired
    MediaLstWithCatalogItemMediaAndMediaFileNameSproc mediaLstWithCatalogItemMediaAndMediaFileNameSproc;

    /**
     * //set all other media  userRank to 0 in LCM
     *
     * @param domainId
     * @param user
     */
    public void unSetOtherMediaHero(int domainId, String user, int mediaId) {
        final List<LcmCatalogItemMedia> lcmCatalogItemMediaList =
                (List<LcmCatalogItemMedia>) catalogItemListSproc.execute(domainId).get(CatalogItemListSproc.MEDIA_SET);
        for (final LcmCatalogItemMedia lcmCatalogItemMedia : lcmCatalogItemMediaList) {
            if (lcmCatalogItemMedia.getMediaId() != mediaId) {
                catalogItemMediaChgSproc.updateCategory(lcmCatalogItemMedia.getCatalogItemId(), lcmCatalogItemMedia.getMediaId(),
                        DEFAULT_USER_RANK, user, ROOM_UPDATED_BY);
            }
        }
    }

    public void setMediaToHero(String user, LcmCatalogItemMedia lcmCatalogItemMedia, boolean hero) {
        if (hero) {
            catalogItemMediaChgSproc.updateCategory(lcmCatalogItemMedia.getCatalogItemId(), lcmCatalogItemMedia.getMediaId(),
                    3, user, ROOM_UPDATED_BY);
        } else {
            catalogItemMediaChgSproc.updateCategory(lcmCatalogItemMedia.getCatalogItemId(), lcmCatalogItemMedia.getMediaId(),
                    DEFAULT_USER_RANK, user, ROOM_UPDATED_BY);
        }

    }

    public LcmCatalogItemMedia getCatalogItemMeida(int catalogItemId, int mediaId) {
        final LcmCatalogItemMedia catalogItemMedia =
                catalogItemMediaGetSproc.getMedia(catalogItemId, mediaId).get(0);
        return catalogItemMedia;
    }

    /**
     * update the current media's useRank in catalogItemMedia table in LCM, if hero, set 3, if not hero, set the input subcategoryId.
     *
     * @param imageMessage
     * @param domainId
     * @param mediaId
     */
    public void updateCurrentMediaHero(ImageMessage imageMessage, int domainId, int mediaId) {
        catalogitemMediaDao.updateCatalogItem(imageMessage, Integer.valueOf(mediaId), domainId);
    }

    /**
     * Update category(MediaUserRank field) to old value found in Dynamo for all hero media belonging to the domain id
     * specified in the imageMessage parameter.
     *
     * @param imageMessage Image message to use
     */
    public void setOldCategoryForHeroPropertyMedia(ImageMessage imageMessage, String domainId, String guid, int mediaId) throws MediaDBException {
        final String catalogItemId = domainId;
        //final List<Media> heroMedia = mediaRepo.retrieveHeroPropertyMedia(catalogItemId, LODGING.getDomain());
        final List<Media> dynamoHeroMedia = mediaRepo.retrieveHeroPropertyMedia(catalogItemId, LODGING.getDomain()).stream()
                .filter(item -> !guid.equals(imageMessage.getMediaGuid()) &&
                        !StringUtils.isEmpty(item.getLcmMediaId()) &&
                        !item.getLcmMediaId().equalsIgnoreCase("null") &&
                        item.getDomainFields() != null)
                .collect(Collectors.toList());
        final List<LcmCatalogItemMedia> lcmHeroMedia = mediaLstWithCatalogItemMediaAndMediaFileNameSproc.getMedia(Integer.parseInt(catalogItemId))
                .stream().filter(item -> item.getMediaId() != mediaId).collect(Collectors.toList());
        final List<CategoryMedia> categoryMediaList = buildCategoryMediaList(lcmHeroMedia, dynamoHeroMedia);

        try {
            for (final CategoryMedia categoryMedia : categoryMediaList) {
                catalogItemMediaChgSproc.updateCategory(Integer.parseInt(catalogItemId), categoryMedia.getLcmMediaId(),
                        categoryMedia.getSubcategoryId(), imageMessage.getUserId(), ROOM_UPDATED_BY);
            }
        } catch (Exception ex) {
            final String error = String.format("ERROR updating previous category to hero image when processing GUID=[%s]",
                    guid);
            throw new MediaDBException(error, ex);
        }
    }

    private List<CategoryMedia> buildCategoryMediaList(List<LcmCatalogItemMedia> lcmMediaList, List<Media> dynamoMediaList) {
        final List<CategoryMedia> categoryMediaList = new ArrayList<>();
        final Map<Integer, Date> lcmMediaMap = new HashMap<>();
        final Set<Integer> dynamoLcmIdSet = new HashSet<>();
        lcmMediaList.forEach(media -> lcmMediaMap.put(media.getMediaId(), media.getLastUpdateDate()));

        dynamoMediaList.stream()
                .filter(media -> (lcmMediaMap.get(Integer.valueOf(media.getLcmMediaId()))) == null ||
                        compareDates(lcmMediaMap.get(Integer.valueOf(media.getLcmMediaId())), media.getLastUpdated()))
                .forEach(media -> {
                    if (media.getDomainFields() != null && media.getLcmMediaId() != null) {
                        final Map map = JSONUtil.buildMapFromJson(media.getDomainFields());
                        final String subcategory = (String) map.get(SUBCATEGORY_ID);
                        final int subcategoryId = Integer.parseInt(StringUtils.isEmpty(subcategory) ? "0" : subcategory);
                        categoryMediaList.add(new CategoryMedia(subcategoryId, Integer.parseInt(media.getLcmMediaId())));
                        dynamoLcmIdSet.add(Integer.valueOf(media.getLcmMediaId()));
                    }
                });
        lcmMediaList.stream().filter(media -> media.getMediaUseRank() == 3 && !dynamoLcmIdSet.contains(media.getMediaId()))
                .forEach(media ->
                        categoryMediaList.add(new CategoryMedia(0, media.getMediaId())));
        return categoryMediaList;
    }

    private Boolean compareDates(Date lcmDate, Date dynamoDate) {
        final ZonedDateTime lcmDateZoned = ZonedDateTime.ofInstant(lcmDate.toInstant(), ZoneId.of("America/Los_Angeles"));
        final ZonedDateTime dynamoDateZoned = ZonedDateTime.ofInstant(dynamoDate.toInstant(), ZoneId.of("UTC"));
        return lcmDateZoned.isBefore(dynamoDateZoned);
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
