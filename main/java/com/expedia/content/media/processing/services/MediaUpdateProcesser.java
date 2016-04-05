package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.CatalogitemMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.MediaUpdateDao;
import com.expedia.content.media.processing.services.dao.domain.LcmCatalogItemMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaRoomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MediaUpdateProcesser {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaUpdateProcesser.class);
    public static final String MESSAGE_PROPERTY_HERO = "propertyHero";
    public static final String MESSAGE_SUB_CATEGORY_ID = "subcategoryId";
    public static final String MESSAGE_ROOMS = "rooms";
    public static final String MESSSAGE_ROOM_HERO = "roomHero";
    @Autowired
    private MediaUpdateDao mediaUpdateDao;
    @Autowired
    private CatalogitemMediaDao catalogitemMediaDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private CatelogHeroProcesser catelogHeroProcesser;

    /**
     * process media update, involve media table, catalogItemMedia table, and paragraph table in lcm.
     * and media table in dynamo
     *
     * @param imageMessage
     * @param mediaId
     * @param domainId
     * @param dynamoMedia  can be null if media only exists in LCM.
     * @return
     * @throws Exception
     */
    @Transactional
    public ResponseEntity<String> processRequest(final ImageMessage imageMessage,
            final String mediaId, String domainId, Media dynamoMedia
    ) throws Exception {
        Integer expediaId;
        if (domainId.isEmpty()) {
            final LcmMedia lcmMedia = mediaUpdateDao.getMediaByMediaId(Integer.valueOf(mediaId));
            expediaId = lcmMedia.getDomainId();
        } else {
            expediaId = Integer.valueOf(domainId);
        }
        //step1. update media table, if commented and active is not null
        if (imageMessage.getComment() != null || imageMessage.isActive() != null) {
            mediaUpdateDao.updateMedia(imageMessage, Integer.valueOf(mediaId));
        }
        //step 2 update property hero in catalotItemMedia
        if (imageMessage.getOuterDomainData() != null &&
                (imageMessage.getOuterDomainData().getDomainFieldValue(MESSAGE_PROPERTY_HERO) != null
                        || imageMessage.getOuterDomainData().getDomainFieldValue(MESSAGE_SUB_CATEGORY_ID) != null)) {

            if (dynamoMedia == null) {
                handleLCMPropertyHero(imageMessage, Integer.valueOf(mediaId), expediaId, "");
            } else {
                handleLCMPropertyHero(imageMessage, Integer.valueOf(mediaId), expediaId, dynamoMedia.getMediaGuid());
            }
        }
        //step 3 update room table.
        processRooms(imageMessage, mediaId);
        LOGGER.info("update  imageMessage=[{}], mediaId=[{}] to LCM done", imageMessage.toJSONMessage(), mediaId);
        //step 4. save media to dynamo
        if (dynamoMedia != null) {
            setDynamMedia(imageMessage, dynamoMedia);
            mediaDao.saveMedia(dynamoMedia);
        }
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    /**
     * process property hero update for media only exists in LCM
     *
     * @param imageMessage
     * @param mediaId
     * @param domainId
     */
    private void handleLCMPropertyHero(ImageMessage imageMessage, int mediaId, int domainId, String guid) {
        //subcategory id is null and hero tag is not null
        final String heroProperty = (String) imageMessage.getOuterDomainData().getDomainFieldValue(MESSAGE_PROPERTY_HERO);
        final String subCategoryId = (String) imageMessage.getOuterDomainData().getDomainFieldValue(MESSAGE_SUB_CATEGORY_ID);
        if (subCategoryId == null
                && heroProperty != null) {
            //hero is true
            handleSubIdNull(imageMessage, mediaId, domainId, guid, heroProperty);
        } else if (subCategoryId != null
                && heroProperty == null) {
            handleHeroNull(imageMessage, mediaId, domainId);
        } else if (subCategoryId != null
                && heroProperty != null) {
            handleBothNotNull(imageMessage, mediaId, domainId, guid, heroProperty);
        }
    }

    private void handleSubIdNull(ImageMessage imageMessage, int mediaId, int domainId, String guid, String heroProperty) {
        if ("true".equalsIgnoreCase(heroProperty)) {
            setHeroImgage(imageMessage, mediaId, domainId);
            unSetHeroImgage(imageMessage, mediaId, domainId, guid);
        } else {
            final LcmCatalogItemMedia lcmCatalogItemMedia = catelogHeroProcesser.getCatalogItemMeida(domainId, mediaId);
            if (lcmCatalogItemMedia.getMediaUseRank() == 3) {
                catelogHeroProcesser.setMediaToHero(imageMessage.getUserId(), lcmCatalogItemMedia, false);
            }
        }
    }

    private void handleBothNotNull(ImageMessage imageMessage, int mediaId, int domainId, String guid, String heroProperty) {
        if ("true".equalsIgnoreCase(heroProperty)) {
            setHeroImgage(imageMessage, mediaId, domainId);
            unSetHeroImgage(imageMessage, mediaId, domainId, guid);
        } else {
            //set the subid from json.
            catelogHeroProcesser.updateCurrentMediaHero(imageMessage, domainId, mediaId);
        }
    }

    private void handleHeroNull(ImageMessage imageMessage, int mediaId, int domainId) {
        final LcmCatalogItemMedia lcmCatalogItemMedia = catelogHeroProcesser.getCatalogItemMeida(domainId, mediaId);
        //if it is not hero now , update with id in JSON.
        if (lcmCatalogItemMedia.getMediaUseRank() != 3) {
            catelogHeroProcesser.updateCurrentMediaHero(imageMessage, domainId, mediaId);
        }
    }

    private void setHeroImgage(ImageMessage imageMessage, int mediaId, int domainId) {
        final LcmCatalogItemMedia lcmCatalogItemMedia = catelogHeroProcesser.getCatalogItemMeida(domainId, mediaId);
        if (lcmCatalogItemMedia.getMediaUseRank() != 3) {
            catelogHeroProcesser.setMediaToHero(imageMessage.getUserId(), lcmCatalogItemMedia, true);
        }
    }

    private void unSetHeroImgage(ImageMessage imageMessage, int mediaId, int domainId, String guid) {
        if (guid.isEmpty()) {
            catelogHeroProcesser.unSetOtherMediaHero(domainId, imageMessage.getUserId(), mediaId);

        } else {
            catelogHeroProcesser.setOldCategoryForHeroPropertyMedia(imageMessage, Integer.toString(domainId), guid, mediaId);

        }
    }

    /**
     * only update part of property in DynamoMedia
     *
     * @param imageMessage
     * @param dynamoMedia
     * @throws Exception
     */
    private void setDynamMedia(ImageMessage imageMessage, Media dynamoMedia) throws Exception {
        List<String> commentList = null;
        if (imageMessage.getComment() != null) {
            commentList = new ArrayList<>();
            commentList.add(imageMessage.getComment());
            FieldUtils.writeField(dynamoMedia, "commentList", commentList, true);
        }
        if (imageMessage.isActive() != null) {
            FieldUtils.writeField(dynamoMedia, "active", imageMessage.isActive() ? "true" : "false", true);
        }
        if (imageMessage.getUserId() != null) {
            FieldUtils.writeField(dynamoMedia, "userId", imageMessage.getUserId(), true);
        }
        final Map<String, Object> domainFieldsDynamo = JSONUtil.buildMapFromJson(dynamoMedia.getDomainFields());
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
        if (domainFieldsNew.isEmpty()) {
            return domainFieldsDynamo;
        } else if (domainFieldsDynamo.isEmpty()) {
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

    private void processRooms(ImageMessage imageMessage, String mediaId) {
        final List<Map> roomList = (List<Map>) imageMessage.getOuterDomainData().getDomainFieldValue(MESSAGE_ROOMS);
        if (roomList == null || roomList.isEmpty()) {
            return;
        }
        final List<LcmMediaRoom> jsonRoomList = convert(roomList);
        //rooms from LCM DB.
        final List<LcmMediaRoom> lcmMediaRoomList = catalogitemMediaDao.getLcmRoomsByMediaId(Integer.valueOf(mediaId));
        //room to be delete
        final List<LcmMediaRoom> deleteRoomListCata = new ArrayList<>();
        final List<LcmMediaRoom> deleteRoomListPara = new ArrayList<>();
        //room need to be add
        final List<LcmMediaRoom> addedRoomListCata = new ArrayList<>();
        final List<LcmMediaRoom> addedRoomListPara = new ArrayList<>();

        MediaRoomUtil.initDataList(jsonRoomList, lcmMediaRoomList, deleteRoomListCata, addedRoomListCata, deleteRoomListPara, addedRoomListPara);

        deleteParagraph(deleteRoomListPara);
        deleteCatalogForRoom(deleteRoomListCata, Integer.valueOf(mediaId));
        addCatalogForRoom(addedRoomListCata, Integer.valueOf(mediaId), imageMessage);
        addParagraph(addedRoomListPara, Integer.valueOf(mediaId));
    }

    private void deleteParagraph(List<LcmMediaRoom> deleteRoomListPara) {
        deleteRoomListPara.stream().forEach(lcmMediaRoom -> {
            catalogitemMediaDao.deleteParagraph(lcmMediaRoom.getRoomId());
        });
    }

    private void addParagraph(List<LcmMediaRoom> addRoomListPara, int mediaId) {
        addRoomListPara.stream().forEach(lcmMediaRoom -> {
            catalogitemMediaDao.addOrUpdateParagraph(lcmMediaRoom.getRoomId(), mediaId);
        });
    }

    private void deleteCatalogForRoom(List<LcmMediaRoom> deleteRoomListCata, int mediaId) {
        deleteRoomListCata.stream().forEach(lcmMediaRoom -> {
            catalogitemMediaDao.deleteCatalogItem(lcmMediaRoom.getRoomId(), mediaId);
        });
    }

    private void addCatalogForRoom(List<LcmMediaRoom> addRoomListCata, int mediaId, ImageMessage imageMessage) {
        addRoomListCata.stream().forEach(lcmMediaRoom -> {
            catalogitemMediaDao.addCatalogItemForRoom(lcmMediaRoom.getRoomId(), mediaId, imageMessage);
        });
    }

    private List<LcmMediaRoom> convert(List<Map> roomList) {
        final List<LcmMediaRoom> lcmMediaRoomList = new ArrayList<>();
        if (roomList != null && !roomList.isEmpty()) {
            roomList.stream().forEach(room -> {
                final boolean hero = (room.get(MESSSAGE_ROOM_HERO)).equals("true") ? true : false;
                final LcmMediaRoom lcmMediaRoom = LcmMediaRoom.builder().roomId(Integer.valueOf((String) room.get("roomId")))
                        .roomHero(hero).build();
                lcmMediaRoomList.add(lcmMediaRoom);
            });
        }
        return lcmMediaRoomList;
    }
}
