package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import org.apache.commons.collections.ListUtils;

import java.util.List;
import java.util.stream.Collectors;

public class MediaRoomUtil {

    private MediaRoomUtil() {
    }

    /**
     * initialize different roomList that need to be add or delete
     *
     * @param jsonRoomList       rooms from input JSON request.
     * @param lcmMediaRoomList   rooms from current LCM DB.
     * @param deleteRoomListCata rooms that need to be removed from catalogItemMedia table.
     * @param addedRoomListCata  room to be added to catalogItemMedia table.
     * @param deleteRoomListPara room to be unassociated from paragraph table.
     * @param addedRoomListPara  room to be added to paragraph table.
     */
    public static void initDataList(List<LcmMediaRoom> jsonRoomList, List<LcmMediaRoom> lcmMediaRoomList,
            List<LcmMediaRoom> deleteRoomListCata, List<LcmMediaRoom> addedRoomListCata,
            List<LcmMediaRoom> deleteRoomListPara, List<LcmMediaRoom> addedRoomListPara) {

        for (final LcmMediaRoom jsonRoom : jsonRoomList) {
            final Boolean containAndEqual = containRoom(lcmMediaRoomList, jsonRoom);
            //DB contain the room and hero value is different
            if (containAndEqual != null && !containAndEqual) {
                if (jsonRoom.getRoomHero()) {
                    addedRoomListPara.add(jsonRoom);
                } else {
                    deleteRoomListPara.add(jsonRoom);
                }
            }
        }
        List<LcmMediaRoom> remainJsonRoomList = ListUtils.subtract(jsonRoomList, addedRoomListPara);
        remainJsonRoomList = ListUtils.subtract(remainJsonRoomList, deleteRoomListPara);
        final List<LcmMediaRoom> newRoomList = ListUtils.subtract(remainJsonRoomList, lcmMediaRoomList);

        //by filter remove the room already in roomListPara
        final List<LcmMediaRoom> remainDBList =
                lcmMediaRoomList.stream().filter(lcmMediaRoom1 -> !containSameId(deleteRoomListPara, lcmMediaRoom1.getRoomId()))
                        .filter(lcmMediaRoom1 -> !containSameId(addedRoomListPara, lcmMediaRoom1.getRoomId()))
                        .collect(Collectors.toList());
        final List<LcmMediaRoom> removeRoomList = ListUtils.subtract(remainDBList, remainJsonRoomList);

        addedRoomListCata.addAll(newRoomList);
        addedRoomListPara.addAll(newRoomList.stream().filter(lcmMediaRoom -> lcmMediaRoom.getRoomHero()).collect(Collectors.toList()));
        deleteRoomListCata.addAll(removeRoomList);
        deleteRoomListPara.addAll(removeRoomList.stream().filter(lcmMediaRoom -> lcmMediaRoom.getRoomHero()).collect(Collectors.toList()));

    }

    public static boolean containSameId(List<LcmMediaRoom> lcmMediaRoomList, int roomid) {
        for (final LcmMediaRoom lcmMediaRoom : lcmMediaRoomList) {
            if (lcmMediaRoom.getRoomId() == roomid) {
                return true;
            }
        }
        return false;
    }

    /**
     * null means does not exist, true means DB contain the room and value is the same,
     * false mean DB contain the room but hero value is different.
     *
     * @param lcmMediaRoomList
     * @param jsonRoom
     * @return
     */
    private static Boolean containRoom(List<LcmMediaRoom> lcmMediaRoomList, LcmMediaRoom jsonRoom) {
        for (final LcmMediaRoom dbRoom : lcmMediaRoomList) {
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
}
