package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.CatalogitemMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.MediaUpdateDao;
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

    @Autowired
    private MediaUpdateDao mediaUpdateDao;
    @Autowired
    private CatalogitemMediaDao catalogitemMediaDao;
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    private CatelogHeroProcesser catelogHeroProcesser;

    @Transactional
    public ResponseEntity<String> processRequest(final ImageMessage imageMessage,
            final String mediaId, String domainId, Media dynamoMedia
    ) throws Exception {
        Integer expediaId = null;
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
                (imageMessage.getOuterDomainData().getDomainFieldValue("propertyHero") != null
                        || imageMessage.getOuterDomainData().getDomainFieldValue("subcategoryId") != null)) {

            if (dynamoMedia == null) {
                handleLCMPropertyHero(imageMessage, Integer.valueOf(mediaId), expediaId);
            } else {
                handleDynamoAndLCMPropertyHero(imageMessage, Integer.valueOf(mediaId), expediaId, dynamoMedia.getMediaGuid());
            }
        }
        //step 3 update room table.
        processRooms(imageMessage, mediaId);
        LOGGER.info("update imageMessage=[{}], mediaId=[{}] done", imageMessage.toJSONMessage(), mediaId);
        //step 4. save media to dynamo
        if (dynamoMedia != null) {
            setDynamMedia(imageMessage, dynamoMedia);
            mediaDao.saveMedia(dynamoMedia);
        }
        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    private void handleLCMPropertyHero(ImageMessage imageMessage, int mediaId, int domainId) {
        if (imageMessage.getOuterDomainData().getDomainFieldValue("subcategoryId") == null
                && imageMessage.getOuterDomainData().getDomainFieldValue("propertyHero") != null) {
            catelogHeroProcesser.unSetOtherMediaHero(domainId, imageMessage.getUserId());
        } else if (imageMessage.getOuterDomainData().getDomainFieldValue("subcategoryId") != null) {
            catelogHeroProcesser.unSetOtherMediaHero(domainId, imageMessage.getUserId());
            catelogHeroProcesser.updateCurrentMediaHero(imageMessage, domainId, mediaId);
        }
    }

    private void handleDynamoAndLCMPropertyHero(ImageMessage imageMessage, int mediaId, int domainId, String guid) {
        if (imageMessage.getOuterDomainData().getDomainFieldValue("subcategoryId") == null
                && imageMessage.getOuterDomainData().getDomainFieldValue("propertyHero") != null) {
            final boolean updateValueWithDynamo = catelogHeroProcesser.setOldCategoryForHeroPropertyMedia(imageMessage, Integer.toString(domainId), guid);
            //if update dynamo fail.
            if (!updateValueWithDynamo) {
                catelogHeroProcesser.unSetOtherMediaHero(domainId, imageMessage.getUserId());
            }
        } else if (imageMessage.getOuterDomainData().getDomainFieldValue("subcategoryId") != null) {
            final boolean updateValueWithDynamo = catelogHeroProcesser.setOldCategoryForHeroPropertyMedia(imageMessage, Integer.toString(domainId), guid);
            //if update dynamo fail.
            if (!updateValueWithDynamo) {
                catelogHeroProcesser.unSetOtherMediaHero(domainId, imageMessage.getUserId());
            }
            catelogHeroProcesser.updateCurrentMediaHero(imageMessage, domainId, mediaId);
        }
    }

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

    private Map<String, Object> combineDomainFields(Map<String, Object> domainFieldsDynamo, Map<String, Object> domainFieldsNew) {
        if (domainFieldsNew.isEmpty()) {
            return domainFieldsDynamo;
        } else if (domainFieldsDynamo.isEmpty()) {
            return domainFieldsNew;
        } else {
            if (domainFieldsNew.get("rooms") != null) {
                domainFieldsDynamo.put("rooms", domainFieldsNew.get("rooms"));
            }
            if (domainFieldsNew.get("subcategoryId") != null) {
                domainFieldsDynamo.put("subcategoryId", domainFieldsNew.get("subcategoryId"));
            }
            if (domainFieldsNew.get("propertyHero") != null) {
                domainFieldsDynamo.put("propertyHero", domainFieldsNew.get("propertyHero"));
            }
            return domainFieldsDynamo;
        }
    }

    private void processRooms(ImageMessage imageMessage, String mediaId) {
        final List<Map> roomList = (List<Map>) imageMessage.getOuterDomainData().getDomainFieldValue("rooms");
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
        roomList.stream().forEach(room -> {
            final boolean hero = (room.get("roomHero")).equals("true") ? true : false;
            final LcmMediaRoom lcmMediaRoom = LcmMediaRoom.builder().roomId(Integer.valueOf((String) room.get("roomId")))
                    .roomHero(hero).build();
            lcmMediaRoomList.add(lcmMediaRoom);
        });
        return lcmMediaRoomList;
    }
}
