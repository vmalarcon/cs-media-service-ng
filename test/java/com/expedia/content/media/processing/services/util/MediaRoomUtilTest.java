package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class MediaRoomUtilTest {

    @Test
    public void testWithInitializeList() {
        LcmMediaRoom req1 = LcmMediaRoom.builder().roomHero(true).roomId(1).build();
        LcmMediaRoom req2 = LcmMediaRoom.builder().roomHero(false).roomId(2).build();
        LcmMediaRoom req3 = LcmMediaRoom.builder().roomHero(true).roomId(3).build();
        LcmMediaRoom req7 = LcmMediaRoom.builder().roomHero(false).roomId(5).build();
        LcmMediaRoom req10 = LcmMediaRoom.builder().roomHero(true).roomId(7).build();

        List<LcmMediaRoom> lcmMediaReqList = new ArrayList<>();
        lcmMediaReqList.add(req1);
        lcmMediaReqList.add(req2);
        lcmMediaReqList.add(req3);
        lcmMediaReqList.add(req7);
        lcmMediaReqList.add(req10);

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

        MediaRoomUtil.initDataList(lcmMediaReqList, lcmMediaDbList, deleteRoomListCata, addedRoomListCata, deleteRoomListPara, addedRoomListPara);
        assertTrue(MediaRoomUtil.containSameId(deleteRoomListCata, 4));
        assertTrue(MediaRoomUtil.containSameId(deleteRoomListCata, 6));

        assertTrue(MediaRoomUtil.containSameId(addedRoomListCata, 1));
        assertTrue(MediaRoomUtil.containSameId(addedRoomListCata, 7));
        assertTrue(MediaRoomUtil.containSameId(addedRoomListCata, 5));

        assertTrue(MediaRoomUtil.containSameId(deleteRoomListPara, 4));
        assertTrue(MediaRoomUtil.containSameId(deleteRoomListPara, 2));

        assertTrue(MediaRoomUtil.containSameId(addedRoomListPara, 1));
        assertTrue(MediaRoomUtil.containSameId(addedRoomListPara, 7));
        assertTrue(MediaRoomUtil.containSameId(addedRoomListPara, 3));
    }
}
