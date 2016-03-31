package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.CatalogitemMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDBException;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaChgSproc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.expedia.content.media.processing.pipeline.domain.Domain.LODGING;

@Component
public class CatelogHeroProcesser {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatelogHeroProcesser.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SUBCATEGORY_ID = "subcategoryId";
    private static final int DEFAULT_USER_RANK = 0;
    private static final String ROOM_UPDATED_BY = "Media Service";
    @Autowired
    private DynamoMediaRepository mediaRepo;
    @Autowired
    private CatalogItemMediaChgSproc catalogItemMediaChgSproc;
    @Autowired
    private CatalogitemMediaDao catalogitemMediaDao;

    private List<Object> getCatalogItemByProperty(int domainId) {
        //todo use CatalogItemMediaLst#01

        return null;
    }

    //set all other media  userRank to 0 in LCM
    public void unSetOtherMediaHero(int domainId, int mediaId) {
        //todo get the List and call unset by loop


    }

    //update the current media to LCM, if hero, set 3, if not hero, set the input subcategoryId.
    public void updateCurrentMediaHero(ImageMessage imageMessage ,int domainId, int mediaId) {
        catalogitemMediaDao.updateCatalogItem(imageMessage, Integer.valueOf(mediaId), domainId);
    }

    /**
     * Update category(MediaUserRank field) to old value found in Dynamo for all hero media belonging to the domain id
     * specified in the imageMessage parameter.
     *
     * @param imageMessage Image message to use
     */
    public boolean setOldCategoryForHeroPropertyMedia(ImageMessage imageMessage, String domainId, String mediaGuid) throws MediaDBException {
//        final OuterDomain outerDomainData = imageMessage.getOuterDomainData();
//        final String propertyHero = getFieldValue(outerDomainData, MESSAGE_PROPERTY_HERO, null);
//        if (!Boolean.parseBoolean(propertyHero)) {
//            return;
//        }

        final String catalogItemId = domainId;
        final List<Media> heroMedia = mediaRepo.retrieveHeroPropertyMedia(catalogItemId, LODGING.getDomain());
        final List<Media> heroLCMMedia = heroMedia.stream()
                .filter(item -> !item.getMediaGuid().equals(mediaGuid) &&
                        !StringUtils.isEmpty(item.getLcmMediaId()) &&
                        !item.getLcmMediaId().equalsIgnoreCase("null") &&
                        item.getDomainFields() != null)
                .collect(Collectors.toList());

        try {
            if (!heroLCMMedia.isEmpty()) {
                for (final Media media : heroLCMMedia) {
                    final Map map = OBJECT_MAPPER.readValue(media.getDomainFields(), Map.class);
                    final String subcategory = (String) map.get(SUBCATEGORY_ID);
                    final int subcategoryId = Integer.parseInt(StringUtils.isEmpty(subcategory) ? "0" : subcategory);
                    catalogItemMediaChgSproc.updateCategory(Integer.parseInt(catalogItemId), Integer.parseInt(media.getLcmMediaId()),
                            subcategoryId, imageMessage.getUserId(), "media service");
                }
                return true;
            } else {
                return false;
            }

        } catch (Exception ex) {
            final String error = String.format("ERROR updating previous category to hero image when processing GUID=[%s]",
                    mediaGuid);
            throw new MediaDBException(error, ex);
        }
    }

    /**
     * Retrieve the value of the field in the imageMessage.
     *
     * @param domain       OuterDomain provided.
     * @param fieldName    field name.
     * @param defaultValue default value.
     * @return Returns the value retrieved.
     */
    private String getFieldValue(OuterDomain domain, String fieldName, String defaultValue) {
        if (domain == null) {
            return defaultValue;
        }
        try {
            if (domain.getDomainFields() != null) {
                final String value = (String) domain.getDomainFieldValue(fieldName);
                return (value == null) ? defaultValue : value;
            }
        } catch (Exception e) {
            LOGGER.warn("The {} is not provided. message= {}", fieldName, e.getMessage(), e);
            return defaultValue;
        }
        return defaultValue;
    }
}
