package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.CatalogitemMediaDao;
import com.expedia.content.media.processing.services.dao.MediaUpdateDao;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import org.apache.commons.collections.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MediaUpdateProcesser {

    @Autowired
    private MediaUpdateDao mediaUpdateDao;
    @Autowired
    private CatalogitemMediaDao catalogitemMediaDao;

    @Transactional
    public ResponseEntity<String> processRequest(final ImageMessage imageMessage, final String requestID, final String serviceUrl, final String clientId,
            final String mediaId
    ) throws Exception {

        //check guid or mediaId
        //validation logic here todo
        // if guid, get mediaID from dynamo, if mediaId, check GUID exist in Dynamo. and how to get expedia id?

        //step1. update media table, if commented and active is not null
        if (imageMessage.getComment() != null || imageMessage.isActive() != null) {
            mediaUpdateDao.updateMedia(imageMessage, Integer.valueOf(mediaId));
        }

        //todo get expediaId from dynamo
        int domainId = 41098;
        //step2 update subcategory id
        if (imageMessage.getOuterDomainData().getDomainFieldValue("subcategoryId") != null) {
            catalogitemMediaDao.updateCatalogItem(imageMessage, Integer.valueOf(mediaId), domainId);
        }
        //step 3 update room table.
        processRooms(imageMessage, mediaId);
        return new ResponseEntity<>("Update Room Successfully.", HttpStatus.OK);
    }

    private void processRooms(ImageMessage imageMessage, String mediaId) {
        List<Map> roomList = (List<Map>) imageMessage.getOuterDomainData().getDomainFieldValue("rooms");
        List<LcmMediaRoom> jsonRoomList = convert(roomList);
        //rooms from LCM DB.
        List<LcmMediaRoom> lcmMediaRoomList = catalogitemMediaDao.getLcmRoomsByMediaId(Integer.valueOf(mediaId));

        List<LcmMediaRoom> deleteRoomListCata = new ArrayList<>();
        List<LcmMediaRoom> addedRoomListCata = new ArrayList<>();
        //add hero room
        List<LcmMediaRoom> deleteRoomListPara = new ArrayList<>();
        //for delete hero room
        List<LcmMediaRoom> addedRoomListPara = new ArrayList<>();

        initDataList(jsonRoomList, lcmMediaRoomList, deleteRoomListCata, addedRoomListCata, deleteRoomListPara, addedRoomListPara);
        deleteParagraph(deleteRoomListPara);
        deleteCatalogForRoom(deleteRoomListCata, Integer.valueOf(mediaId));
        addCatalogForRoom(addedRoomListCata, Integer.valueOf(mediaId), imageMessage);
        addParagraph(addedRoomListPara, Integer.valueOf(mediaId));
    }

    private static void initDataList(List<LcmMediaRoom> jsonRoomList, List<LcmMediaRoom> lcmMediaRoomList,
            List<LcmMediaRoom> deleteRoomListCata, List<LcmMediaRoom> addedRoomListCata,
            List<LcmMediaRoom> deleteRoomListPara, List<LcmMediaRoom> addedRoomListPara) {

        for (LcmMediaRoom jsonRoom : jsonRoomList) {
            Boolean containAndEqual = containRoom(lcmMediaRoomList, jsonRoom);
            if (containAndEqual != null && containAndEqual == false) {
                if (jsonRoom.getRoomHero() == true) {
                    addedRoomListPara.add(jsonRoom);
                } else {
                    deleteRoomListPara.add(jsonRoom);
                }
            }
        }
        List<LcmMediaRoom> remainJsonRoomList = ListUtils.subtract(jsonRoomList, addedRoomListPara);
        remainJsonRoomList = ListUtils.subtract(remainJsonRoomList, deleteRoomListPara);
        List<LcmMediaRoom> newRoomList = ListUtils.subtract(remainJsonRoomList, lcmMediaRoomList);

        List<LcmMediaRoom> remainDBList =
                lcmMediaRoomList.stream().filter(lcmMediaRoom1 -> containSameId(deleteRoomListPara, lcmMediaRoom1.getRoomId()) == false)
                        .filter(lcmMediaRoom1 -> containSameId(addedRoomListPara, lcmMediaRoom1.getRoomId()) == false)
                        .collect(Collectors.toList());
        List<LcmMediaRoom> removeRoomList = ListUtils.subtract(remainDBList, remainJsonRoomList);

        addedRoomListCata.addAll(newRoomList);
        addedRoomListPara.addAll(newRoomList.stream().filter(lcmMediaRoom -> lcmMediaRoom.getRoomHero() == true).collect(Collectors.toList()));
        deleteRoomListCata.addAll(removeRoomList);
        deleteRoomListPara.addAll(removeRoomList.stream().filter(lcmMediaRoom -> lcmMediaRoom.getRoomHero() == true).collect(Collectors.toList()));

    }

    private static boolean containSameId(List<LcmMediaRoom> lcmMediaRoomList, int roomid) {
        for (LcmMediaRoom lcmMediaRoom : lcmMediaRoomList) {
            if (lcmMediaRoom.getRoomId() == roomid) {
                return true;
            }
        }
        return false;
    }

    /**
     * null means does not exist, true means contail and value is the same
     *
     * @param lcmMediaRoomList
     * @param jsonRoom
     * @return
     */
    private static Boolean containRoom(List<LcmMediaRoom> lcmMediaRoomList, LcmMediaRoom jsonRoom) {
        for (LcmMediaRoom dbRoom : lcmMediaRoomList) {
            //exactly the same, that means we do not need to update.
            if (jsonRoom.getRoomId() == dbRoom.getRoomId() && jsonRoom.getRoomHero() == dbRoom.getRoomHero()) {
                return true;
            }
            if (jsonRoom.getRoomId() == dbRoom.getRoomId() && jsonRoom.getRoomHero() != dbRoom.getRoomHero()) {
                return false;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        LcmMediaRoom req1 = LcmMediaRoom.builder().roomHero(true).roomId(1).build();
        LcmMediaRoom req2 = LcmMediaRoom.builder().roomHero(false).roomId(2).build();
        LcmMediaRoom req3 = LcmMediaRoom.builder().roomHero(true).roomId(3).build();
        LcmMediaRoom req7 = LcmMediaRoom.builder().roomHero(false).roomId(5).build();

        List<LcmMediaRoom> lcmMediaReqList = new ArrayList<>();
        lcmMediaReqList.add(req1);
        lcmMediaReqList.add(req2);
        lcmMediaReqList.add(req3);
        lcmMediaReqList.add(req7);

        LcmMediaRoom req4 = LcmMediaRoom.builder().roomHero(true).roomId(4).build();
        LcmMediaRoom req5 = LcmMediaRoom.builder().roomHero(true).roomId(2).build();
        LcmMediaRoom req6 = LcmMediaRoom.builder().roomHero(false).roomId(3).build();
        LcmMediaRoom req8 = LcmMediaRoom.builder().roomHero(false).roomId(6).build();

        List<LcmMediaRoom> lcmMediaDbList = new ArrayList<>();
        lcmMediaDbList.add(req4);
        lcmMediaDbList.add(req5);
        lcmMediaDbList.add(req6);
        lcmMediaDbList.add(req8);

        List<LcmMediaRoom> deleteRoomListCata = new ArrayList<>();
        List<LcmMediaRoom> addedRoomListCata = new ArrayList<>();

        List<LcmMediaRoom> deleteRoomListPara = new ArrayList<>();
        List<LcmMediaRoom> addedRoomListPara = new ArrayList<>();

        initDataList(lcmMediaReqList, lcmMediaDbList, deleteRoomListCata, addedRoomListCata, deleteRoomListPara, addedRoomListPara);

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
        List<LcmMediaRoom> lcmMediaRoomList = new ArrayList<>();
        roomList.stream().forEach(room -> {
            boolean hero = (room.get("roomHero")).equals("true") ? true : false;
            LcmMediaRoom lcmMediaRoom = LcmMediaRoom.builder().roomId(Integer.valueOf((String) room.get("roomId")))
                    .roomHero(hero).build();
            lcmMediaRoomList.add(lcmMediaRoom);
        });
        return lcmMediaRoomList;
    }
}
