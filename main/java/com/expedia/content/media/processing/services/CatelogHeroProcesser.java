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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.expedia.content.media.processing.pipeline.domain.Domain.LODGING;

@Component
public class CatelogHeroProcesser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SUBCATEGORY_ID = "subcategoryId";
    private static final int DEFAULT_USER_RANK = 0;
    private static final String ROOM_UPDATED_BY = "Media Service";
    private static final int GMT_PST_HOUR_DIFFERENCE = 7;
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

    //set all other media  userRank to 0 in LCM
    public void unSetOtherMediaHero(int domainId, String user) {
        final List<LcmCatalogItemMedia> lcmCatalogItemMediaList =
                (List<LcmCatalogItemMedia>) catalogItemListSproc.execute(domainId).get(CatalogItemListSproc.MEDIA_SET);
        for (final LcmCatalogItemMedia lcmCatalogItemMedia : lcmCatalogItemMediaList) {
            catalogItemMediaChgSproc.updateCategory(lcmCatalogItemMedia.getCatalogItemId(), lcmCatalogItemMedia.getMediaId(),
                    DEFAULT_USER_RANK, user, ROOM_UPDATED_BY);
        }
    }

    //update the current media to LCM, if hero, set 3, if not hero, set the input subcategoryId.
    public void updateCurrentMediaHero(ImageMessage imageMessage, int domainId, int mediaId) {
        catalogitemMediaDao.updateCatalogItem(imageMessage, Integer.valueOf(mediaId), domainId);
    }

    /**
     * Update category(MediaUserRank field) to old value found in Dynamo for all hero media belonging to the domain id
     * specified in the imageMessage parameter.
     *
     * @param imageMessage Image message to use
     */
    public boolean setOldCategoryForHeroPropertyMedia(ImageMessage imageMessage, String domainId, String mediaGuid) throws MediaDBException {
        final String catalogItemId = domainId;
        final List<Media> heroMedia = mediaRepo.retrieveHeroPropertyMedia(catalogItemId, LODGING.getDomain());
        final List<Media> heroLCMMedia = heroMedia.stream()
                .filter(item -> !item.getMediaGuid().equals(mediaGuid) &&
                        !StringUtils.isEmpty(item.getLcmMediaId()) &&
                        !item.getLcmMediaId().equalsIgnoreCase("null") &&
                        item.getDomainFields() != null)
                .collect(Collectors.toList());

        try {
            if (heroLCMMedia.isEmpty()) {
                return false;
            } else {
                for (final Media media : heroLCMMedia) {
                    final Map map = OBJECT_MAPPER.readValue(media.getDomainFields(), Map.class);
                    final String subcategory = (String) map.get(SUBCATEGORY_ID);
                    final int subcategoryId = Integer.parseInt(StringUtils.isEmpty(subcategory) ? "0" : subcategory);
                    final LcmCatalogItemMedia catalogItemMedia =
                            catalogItemMediaGetSproc.getMedia(Integer.parseInt(catalogItemId), Integer.parseInt(media.getLcmMediaId())).get(0);
                    final boolean isDynamoCategoryNewer =
                            DateUtils.addHours(catalogItemMedia.getLastUpdateDate(), GMT_PST_HOUR_DIFFERENCE).compareTo(media.getLastUpdated()) < 0;
                    if (catalogItemMedia.getMediaUseRank() == 3 || isDynamoCategoryNewer) {
                        catalogItemMediaChgSproc.updateCategory(Integer.parseInt(catalogItemId), Integer.parseInt(media.getLcmMediaId()),
                                subcategoryId, imageMessage.getUserId(), ROOM_UPDATED_BY);
                    }

                }
                return true;
            }

        } catch (Exception ex) {
            final String error = String.format("ERROR updating previous category to hero image when processing GUID=[%s]",
                    mediaGuid);
            throw new MediaDBException(error, ex);
        }
    }

}
