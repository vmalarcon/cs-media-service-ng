package com.expedia.content.media.processing.services.dao;

import com.amazonaws.util.StringUtils;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import com.expedia.content.media.processing.services.dao.domain.Paragraph;
import com.expedia.content.media.processing.services.dao.sql.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Media data access operations through LCM and the Dynamo MediaDB.
 */
@Component
public class LcmDynamoCatalogAndParagraphDao implements CatalogItemMediaDao {

    private static final String DEFAULT_SUBCATEGORY_ID = "0";
    public static final String MESSAGE_PROPERTY_HERO = "propertyHero";
    private static final String PROPERTY_HERO_IMAGE = "3";
    private static final String SUBCATEGORY_ID = "subcategoryId";
    private static final int MEDIA_USE_TYPE_IMAGE = 1;
    public static final int P_SECTION_TYPE_ID = 3;
    public static final int P_PARAGRAPH_NBR = 1;
    private static final String DEFAULT_LANG_ID = "1033";
    private static final int PARAGRAPH_TYPE_ID = 1;
    private static final String DEFAULT_PARAGRAPHTXT = "";
    private static final int CONTENT_PROVIDER_ID = 1;
    private static final String ROOM_UPDATED_BY = "Media Service";
    private static final int CONTENT_SOURCETYPE_ID = 0;

    @Autowired
    private CatalogItemMediaChgSproc catalogItemMediaChgSproc;
    @Autowired
    private AddCatalogItemMediaForRoomsAndRatePlansSproc addCatalogItemMediaForRoom;
    @Autowired
    private SQLRoomGetByMediaIdSproc roomGetSproc;
    @Autowired
    private CatalogItemMediaDelSproc catalogItemMediaDelSproc;
    @Autowired
    private AddParagraphSproc addParagraphSproc;
    @Autowired
    private SQLMediaItemGetSproc lcmMediaItemSproc;
    @Autowired
    private GetParagraphSproc getParagraphSproc;
    @Autowired
    private SetParagraphSproc setParagraphSproc;

    public void updateCatalogItem(ImageMessage imageMessage, int mediaId, int domainId) {
        final String subcategory = resolveCategory(imageMessage.getOuterDomainData().getDomainFields());
        catalogItemMediaChgSproc.updateCategory(domainId, mediaId, Integer.valueOf(subcategory), imageMessage.getUserId(),
                ROOM_UPDATED_BY);

    }

    /**
     * Resolve the category to be stored using domain data values.
     *
     * @param domainFields Map containing all domain data.
     * @return The category to be stored.
     */
    private String resolveCategory(Map<String, Object> domainFields) {
        if (domainFields == null) {
            return DEFAULT_SUBCATEGORY_ID;
        }

        final boolean isPropertyHero = Boolean.parseBoolean((String) domainFields.get(MESSAGE_PROPERTY_HERO));
        if (isPropertyHero) {
            return PROPERTY_HERO_IMAGE;
        }
        return (String) domainFields.getOrDefault(SUBCATEGORY_ID, DEFAULT_SUBCATEGORY_ID);
    }

    public void deleteCatalogItem(final int catalogItemId, final int mediaId) {
        catalogItemMediaDelSproc.deleteCategory(catalogItemId, mediaId);
    }

    @SuppressWarnings({"unchecked", "PMD.NPathComplexity"})
    public void addCatalogItemForRoom(final int roomId, int mediaId, Integer expediaId, ImageMessage imageMessage) {
        final Map<String, Object> mediaResult = lcmMediaItemSproc.execute(expediaId, mediaId);
        final List<LcmMedia> mediaResultList = mediaResult.get(SQLMediaItemGetSproc.MEDIA_SET) == null ? null : (List<LcmMedia>) mediaResult.get(SQLMediaItemGetSproc.MEDIA_SET);
        final LcmMedia media = (mediaResultList.isEmpty()) ? null : ((List<LcmMedia>) mediaResult.get(SQLMediaItemGetSproc.MEDIA_SET)).get(0);

        addCatalogItemMediaForRoom.addCatalogItemMedia(
                roomId,
                mediaId,
                media == null || media.getCategory() == null ? 0 : media.getCategory(),
                true,
                MEDIA_USE_TYPE_IMAGE,
                StringUtils.isNullOrEmpty(imageMessage.getUserId()) ? imageMessage.getClientId() : imageMessage.getUserId(),
                false,
                true,
                ROOM_UPDATED_BY, imageMessage);
    }

    public List<LcmMediaRoom> getLcmRoomsByMediaId(final int mediaId) {
        final Map<String, Object> roomResult = roomGetSproc.execute(mediaId);
        return (List<LcmMediaRoom>) roomResult.get(SQLRoomGetByMediaIdSproc.ROOM_SET);
    }

    public void deleteParagraph(final int roomId) {
        final List<Paragraph> paragraphList = getParagraphSproc.getParagraph(roomId);
        if (!paragraphList.isEmpty()) {
            final Paragraph paragraph = paragraphList.get(0);
            //set the mediaID to null in paragraph table.
            setParagraphSproc.setParagraph(paragraph.getCatalogItemId(), paragraph.getSectionTypeId(), paragraph.getParagraphNbr(),
                    null, Integer.parseInt(DEFAULT_LANG_ID), paragraph.getParagraphTxt(), paragraph.getEffectiveStartDate(),
                    paragraph.getEffectiveEndDate(), null, paragraph.getParagraphTypeId(), null,
                    paragraph.getLastUpdatedBy(), paragraph.getLastUpdateLocation(), null);
        }
    }

    public void addOrUpdateParagraph(final int roomId, final int mediaId) {
        final List<Paragraph> paragraphList = getParagraphSproc.getParagraph(roomId);
        if (paragraphList.isEmpty()) {
            addParagraphSproc
                    .addParagraph(roomId, P_SECTION_TYPE_ID,
                            P_PARAGRAPH_NBR, Integer.parseInt(DEFAULT_LANG_ID),
                            PARAGRAPH_TYPE_ID, DEFAULT_PARAGRAPHTXT,
                            null, null, mediaId, CONTENT_PROVIDER_ID, null, ROOM_UPDATED_BY, ROOM_UPDATED_BY, CONTENT_SOURCETYPE_ID);

        } else {
            final Paragraph paragraph = paragraphList.get(0);
            setParagraphSproc.setParagraph(paragraph.getCatalogItemId(), P_SECTION_TYPE_ID, paragraph.getParagraphNbr(),
                    mediaId, Integer.parseInt(DEFAULT_LANG_ID), paragraph.getParagraphTxt(), paragraph.getEffectiveStartDate(),
                    paragraph.getEffectiveEndDate(), null, paragraph.getParagraphTypeId(), null,
                    paragraph.getLastUpdatedBy(), paragraph.getLastUpdateLocation(), null);
        }
    }

}
