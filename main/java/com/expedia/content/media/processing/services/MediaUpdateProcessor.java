package com.expedia.content.media.processing.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.util.StringUtils;
import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.retry.RetryableMethod;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.dao.CatalogItemMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.MediaUpdateDao;
import com.expedia.content.media.processing.services.dao.domain.LcmCatalogItemMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaRoomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MediaUpdateProcessor {
    private static final FormattedLogger LOGGER = new FormattedLogger(MediaUpdateProcessor.class);
    public static final String MESSAGE_PROPERTY_HERO = "propertyHero";
    public static final String MESSAGE_SUB_CATEGORY_ID = "subcategoryId";
    public static final String MESSAGE_ROOMS = "rooms";
    public static final String MESSAGE_ROOM_HERO = "roomHero";

    @Autowired
    private MediaUpdateDao mediaUpdateDao;
    @Autowired
    private CatalogItemMediaDao catalogItemMediaDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private CatalogHeroProcessor catalogHeroProcessor;
    @Autowired
    private KafkaPublisher kafkaPublisher;

    /**
     * process media update, involve media table, catalogItemMedia table, and paragraph table in lcm.
     * and media table in dynamo
     *
     * @param imageMessage
     * @param mediaId
     * @param domainId
     * @param dynamoMedia can be null if media only exists in LCM.
     * @return
     * @throws Exception
     */
    @Transactional
    public ResponseEntity<String> processRequest(final ImageMessage imageMessage, final String mediaId, String domainId,
                                                 Media dynamoMedia) throws Exception {
        // Only proceed to the following if the domain is Lodging
        if (imageMessage.getOuterDomainData().getDomain().equals(Domain.LODGING) && mediaId != null && org.apache.commons.lang3.StringUtils.isNumeric(mediaId)) {
            final Integer expediaId = Integer.valueOf(domainId);
            // step1. update media table, if comment or active fields are not null
            if (imageMessage.getComment() != null || imageMessage.isActive() != null) {
                mediaUpdateDao.updateMedia(imageMessage, Integer.valueOf(mediaId));
            }
            processCategory(imageMessage, mediaId, dynamoMedia, expediaId);
            updateCatelogItemTimestamp(imageMessage, mediaId, domainId);

        }
        // step 4. save media to dynamo
        if (dynamoMedia != null) {
            final Boolean active = imageMessage.isActive();
            if (active != null) {
                dynamoMedia.setActive(active.toString());
            }
            setDynamoMedia(imageMessage, dynamoMedia);
            dynamoMedia.setLastUpdated(new Date());
            dynamoMedia.setHidden(imageMessage.getHidden());
            mediaDao.saveMedia(dynamoMedia);
        }
        final Map<String, Object> response = new HashMap<>();
        response.put("status", Integer.valueOf(200));
        kafkaPublisher.publishToTopic(imageMessage);
        final String jsonResponse = new ObjectMapper().writeValueAsString(response);
        return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
    }

    /**
     * this method will make the table column 'lastUpdatedBy' and 'updateTime'
     * consistent in Media and catalogitemMedia table
     * even the update request only update one of them.
     * @param imageMessage
     * @param mediaId
     * @param domainId
     */
    private void updateCatelogItemTimestamp(final ImageMessage imageMessage, final String mediaId, final String domainId) {
        if ((imageMessage.isActive() != null || imageMessage.getComment() != null) && imageMessage.getOuterDomainData().getDomainFields() == null) {
            final LcmCatalogItemMedia lcmCatalogItemMedia = catalogHeroProcessor.getCatalogItemMeida(Integer.valueOf(domainId), Integer.valueOf(mediaId));
            catalogHeroProcessor.updateTimestamp(imageMessage.getUserId(), lcmCatalogItemMedia);
        }
        if (imageMessage.isActive() == null && imageMessage.getComment() == null && imageMessage.getOuterDomainData().getDomainFields() != null) {
            final LcmMedia lcmMedia = mediaUpdateDao.getMediaByMediaId(Integer.valueOf(mediaId));
            mediaUpdateDao.updateMediaTimestamp(lcmMedia, imageMessage);
        }
    }

    @RetryableMethod
    private void processCategory(final ImageMessage imageMessage, final String mediaId, Media dynamoMedia, Integer expediaId)
            throws Exception {
        try {
            if (imageMessage.getOuterDomainData() != null && (imageMessage.getOuterDomainData().getDomainFieldValue(MESSAGE_PROPERTY_HERO) != null
                    || imageMessage.getOuterDomainData().getDomainFieldValue(MESSAGE_SUB_CATEGORY_ID) != null)) {
                handleLCMPropertyHero(imageMessage, Integer.valueOf(mediaId), expediaId, dynamoMedia);
            }
            // step 3 update room table.
            processRooms(imageMessage, mediaId, expediaId);
            LOGGER.info("LCM UPDATE DONE mediaId={}", Arrays.asList(mediaId), imageMessage);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * process property hero update for media only exists in LCM
     *
     * @param imageMessage
     * @param mediaId
     * @param domainId
     */
    private void handleLCMPropertyHero(ImageMessage imageMessage, int mediaId, int domainId, Media dynamoMedia) {
        //subcategory id is null and hero tag is not null
        final String heroProperty = (String) imageMessage.getOuterDomainData().getDomainFieldValue(MESSAGE_PROPERTY_HERO);
        final String subCategoryId = (String) imageMessage.getOuterDomainData().getDomainFieldValue(MESSAGE_SUB_CATEGORY_ID);
        if (subCategoryId == null && heroProperty != null) {
            // hero is true
            handleSubcategoryIdNull(imageMessage, mediaId, domainId, dynamoMedia, heroProperty);
        } else if (subCategoryId != null && heroProperty == null) {
            handleHeroNull(imageMessage, mediaId, domainId);
        } else if (subCategoryId != null && heroProperty != null) {
            handleHeroAndSubcategoryIdValid(imageMessage, mediaId, domainId, dynamoMedia, heroProperty);
        }
    }

    @SuppressWarnings({"PMD.NPathComplexity"})
    private void handleSubcategoryIdNull(ImageMessage imageMessage, int mediaId, int domainId, Media dynamoMedia, String heroProperty) {
        final String guid = dynamoMedia == null ? "" : dynamoMedia.getMediaGuid();
        final String domain = (imageMessage.getOuterDomainData() == null ? "" : imageMessage.getOuterDomainData().getDomain().getDomain());
        if ("true".equalsIgnoreCase(heroProperty)) {
            setHeroImage(imageMessage, mediaId, domainId);
            unsetHeroImage(imageMessage, mediaId, domainId, guid, domain);
        } else {
            final LcmCatalogItemMedia lcmCatalogItemMedia = catalogHeroProcessor.getCatalogItemMeida(domainId, mediaId);
            String subcategory = "";
            //if we have subid in dynamo, we need to set that value.
            if (dynamoMedia != null) {
                if (dynamoMedia.getDomainFields() != null) {
                    final Map map = JSONUtil.buildMapFromJson(dynamoMedia.getDomainFields());
                    subcategory = (String) map.get("subcategoryId");
                }
            }
            if (lcmCatalogItemMedia != null && lcmCatalogItemMedia.getMediaUseRank() == 3) {
                catalogHeroProcessor.setMediaToHero(imageMessage.getUserId(), lcmCatalogItemMedia, false, subcategory);
            }
        }
    }

    private void handleHeroAndSubcategoryIdValid(ImageMessage imageMessage, int mediaId, int domainId, Media dynamoMedia, String heroProperty) {
        final String guid = dynamoMedia == null ? "" : dynamoMedia.getMediaGuid();
        final String domain = (imageMessage.getOuterDomainData() == null ? "" : imageMessage.getOuterDomainData().getDomain().getDomain());
        if ("true".equalsIgnoreCase(heroProperty)) {
            setHeroImage(imageMessage, mediaId, domainId);
            unsetHeroImage(imageMessage, mediaId, domainId, guid, domain);
        } else {
            //set the subid from json.
            catalogHeroProcessor.updateCurrentMediaHero(imageMessage, domainId, mediaId);
        }
    }

    private void handleHeroNull(ImageMessage imageMessage, int mediaId, int domainId) {
        final LcmCatalogItemMedia lcmCatalogItemMedia = catalogHeroProcessor.getCatalogItemMeida(domainId, mediaId);
        //if it is not hero now , update with id in JSON.
        if (lcmCatalogItemMedia != null && lcmCatalogItemMedia.getMediaUseRank() != 3) {
            catalogHeroProcessor.updateCurrentMediaHero(imageMessage, domainId, mediaId);
        }
    }

    private void setHeroImage(ImageMessage imageMessage, int mediaId, int domainId) {
        final LcmCatalogItemMedia lcmCatalogItemMedia = catalogHeroProcessor.getCatalogItemMeida(domainId, mediaId);
        if (lcmCatalogItemMedia != null && lcmCatalogItemMedia.getMediaUseRank() != 3) {
            catalogHeroProcessor.setMediaToHero(imageMessage.getUserId(), lcmCatalogItemMedia, true, "");
        }
    }

    private void unsetHeroImage(ImageMessage imageMessage, int mediaId, int domainId, String guid, String domain) {
        if (Domain.LODGING.getDomain().equalsIgnoreCase(domain)) {
            catalogHeroProcessor.setOldCategoryForHeroPropertyMedia(imageMessage, Integer.toString(domainId), guid, mediaId);
        }
    }

    /**
     * only update part of property in DynamoMedia
     *
     * @param imageMessage
     * @param dynamoMedia
     * @throws Exception
     */
    @SuppressWarnings("PMD.NPathComplexity")
    private void setDynamoMedia(ImageMessage imageMessage, Media dynamoMedia) throws Exception {
        if (imageMessage.getComment() != null) {
            final List<String> commentList = new ArrayList<>();
            commentList.add(imageMessage.getComment());
            FieldUtils.writeField(dynamoMedia, "commentList", commentList, true);
        }
        if (imageMessage.isActive() != null) {
            FieldUtils.writeField(dynamoMedia, "active", imageMessage.isActive() ? "true" : "false", true);
        }

        FieldUtils.writeField(dynamoMedia, "userId", StringUtils.isNullOrEmpty(imageMessage.getUserId()) ? imageMessage.getClientId() : imageMessage.getUserId(), true);
        final String domainFields = dynamoMedia.getDomainFields();
        final Map<String, Object> domainFieldsDynamo = domainFields == null ? new HashMap<>() : JSONUtil.buildMapFromJson(domainFields);
        final Map<String, Object> domainFieldsNew = imageMessage.getOuterDomainData().getDomainFields();
        final Map<String, Object> domainFieldsCombine = combineDomainFields(domainFieldsDynamo, domainFieldsNew);
        FieldUtils.writeField(dynamoMedia, "domainFields", new ObjectMapper().writeValueAsString(domainFieldsCombine), true);

    }

    /**
     * replace 'room','subcategoryId','propertyHero' the domainFields value with the input JSON message.
     *
     * @param domainFieldsDynamo
     * @param domainFieldsNew
     * @return
     */
    private Map<String, Object> combineDomainFields(Map<String, Object> domainFieldsDynamo, Map<String, Object> domainFieldsNew) {
        if (domainFieldsNew == null || domainFieldsNew.isEmpty()) {
            return domainFieldsDynamo;
        } else if (domainFieldsDynamo == null || domainFieldsDynamo.isEmpty()) {
            return domainFieldsNew;
        } else {
            if (domainFieldsNew.get(MESSAGE_ROOMS) != null) {
                domainFieldsDynamo.put(MESSAGE_ROOMS, domainFieldsNew.get(MESSAGE_ROOMS));
            }
            if (domainFieldsNew.get(MESSAGE_SUB_CATEGORY_ID) != null) {
                domainFieldsDynamo.put(MESSAGE_SUB_CATEGORY_ID, domainFieldsNew.get(MESSAGE_SUB_CATEGORY_ID));
            }
            if (domainFieldsNew.get(MESSAGE_PROPERTY_HERO) != null) {
                domainFieldsDynamo.put(MESSAGE_PROPERTY_HERO, domainFieldsNew.get(MESSAGE_PROPERTY_HERO));
            }
            return domainFieldsDynamo;
        }
    }

    private void processRooms(ImageMessage imageMessage, String mediaId, Integer expediaId) {
        final List<Map> roomList = (List<Map>) imageMessage.getOuterDomainData().getDomainFieldValue(MESSAGE_ROOMS);
        if (roomList == null) {
            return;
        }
        final List<LcmMediaRoom> jsonRoomList = convert(roomList);
        //rooms from LCM DB.
        final List<LcmMediaRoom> lcmMediaRoomList = catalogItemMediaDao.getLcmRoomsByMediaId(Integer.valueOf(mediaId));
        // room to delete
        final List<LcmMediaRoom> deleteRoomListCata = new ArrayList<>();
        final List<LcmMediaRoom> deleteRoomListPara = new ArrayList<>();
        // room to add
        final List<LcmMediaRoom> addedRoomListCata = new ArrayList<>();
        final List<LcmMediaRoom> addedRoomListPara = new ArrayList<>();

        MediaRoomUtil.initDataList(jsonRoomList, lcmMediaRoomList, deleteRoomListCata, addedRoomListCata, deleteRoomListPara, addedRoomListPara);

        deleteParagraph(deleteRoomListPara);
        deleteCatalogForRoom(deleteRoomListCata, Integer.valueOf(mediaId));
        addCatalogForRoom(addedRoomListCata, Integer.valueOf(mediaId), expediaId, imageMessage);
        addParagraph(addedRoomListPara, Integer.valueOf(mediaId));
    }

    private void deleteParagraph(List<LcmMediaRoom> deleteRoomListPara) {
        deleteRoomListPara.stream().forEach(lcmMediaRoom -> {
            catalogItemMediaDao.deleteParagraph(lcmMediaRoom.getRoomId());
        });
    }

    private void addParagraph(List<LcmMediaRoom> addRoomListPara, int mediaId) {
        addRoomListPara.stream().forEach(lcmMediaRoom -> {
            catalogItemMediaDao.addOrUpdateParagraph(lcmMediaRoom.getRoomId(), mediaId);
        });
    }

    private void deleteCatalogForRoom(List<LcmMediaRoom> deleteRoomListCata, int mediaId) {
        deleteRoomListCata.stream().forEach(lcmMediaRoom -> {
            catalogItemMediaDao.deleteCatalogItem(lcmMediaRoom.getRoomId(), mediaId);
        });
    }

    private void addCatalogForRoom(List<LcmMediaRoom> addRoomListCata, int mediaId, Integer expediaId, ImageMessage imageMessage) {
        addRoomListCata.stream().forEach(lcmMediaRoom -> {
            catalogItemMediaDao.addCatalogItemForRoom(lcmMediaRoom.getRoomId(), mediaId, expediaId, imageMessage);
        });
    }

    private List<LcmMediaRoom> convert(List<Map> roomList) {
        final List<LcmMediaRoom> lcmMediaRoomList = new ArrayList<>();
        if (roomList != null && !roomList.isEmpty()) {
            roomList.stream().forEach(room -> {
                if (room.size() > 0) {
                    final boolean hero = ("true").equals(room.get(MESSAGE_ROOM_HERO)) ? true : false;
                    final LcmMediaRoom lcmMediaRoom = LcmMediaRoom.builder().roomId(Integer.valueOf((String) room.get("roomId")))
                            .roomHero(hero).build();
                    lcmMediaRoomList.add(lcmMediaRoom);
                }

            });
        }
        return lcmMediaRoomList;
    }
}
