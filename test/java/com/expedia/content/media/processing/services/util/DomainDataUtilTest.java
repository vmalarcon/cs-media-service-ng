package com.expedia.content.media.processing.services.util;


import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DomainDataUtilTest {

    @Test
    public void testEmptyRoomsList() {
        final OuterDomain outerDomain = new OuterDomain.OuterDomainBuilder()
                .addField("rooms", new ArrayList<>())
                .build();
        assertFalse(DomainDataUtil.roomsFieldIsInvalid(outerDomain));
    }

    @Test
    public void testEmptyObjectInRoomsList() {
        List<Map<String, String>> roomsMapList = new ArrayList<>();
        roomsMapList.add(new HashMap<>());
        final OuterDomain outerDomain = new OuterDomain.OuterDomainBuilder()
                .addField("rooms", roomsMapList)
                .build();
        assertFalse(DomainDataUtil.roomsFieldIsInvalid(outerDomain));
    }

    @Test
    public void testOnlyRoomId() {
        List<Map<String, String>> roomsMapList = new ArrayList<>();
        Map<String, String> roomMap = new HashMap<>();
        roomMap.put("roomId", "102138123");
        roomsMapList.add(roomMap);
        final OuterDomain outerDomain = new OuterDomain.OuterDomainBuilder()
                .addField("rooms", roomsMapList)
                .build();
        assertFalse(DomainDataUtil.roomsFieldIsInvalid(outerDomain));
    }

    @Test
    public void testOnlyRoomHero() {
        List<Map<String, String>> roomsMapList = new ArrayList<>();
        Map<String, String> roomMap = new HashMap<>();
        roomMap.put("roomHero", "false");
        roomsMapList.add(roomMap);
        final OuterDomain outerDomain = new OuterDomain.OuterDomainBuilder()
                .addField("rooms", roomsMapList)
                .build();
        assertTrue(DomainDataUtil.roomsFieldIsInvalid(outerDomain));
    }
}
