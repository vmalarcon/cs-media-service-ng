package com.expedia.content.media.processing.services.util;


import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.domain.Media;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        final List<Map<String, String>> roomsMapList = new ArrayList<>();
        roomsMapList.add(new HashMap<>());
        final OuterDomain outerDomain = new OuterDomain.OuterDomainBuilder()
                .addField("rooms", roomsMapList)
                .build();
        assertFalse(DomainDataUtil.roomsFieldIsInvalid(outerDomain));
    }

    @Test
    public void testOnlyRoomId() {
        final List<Map<String, String>> roomsMapList = new ArrayList<>();
        final Map<String, String> roomMap = new HashMap<>();
        roomMap.put("roomId", "102138123");
        roomsMapList.add(roomMap);
        final OuterDomain outerDomain = new OuterDomain.OuterDomainBuilder()
                .addField("rooms", roomsMapList)
                .build();
        assertFalse(DomainDataUtil.roomsFieldIsInvalid(outerDomain));
    }
    
    @Test
    public void testEmptyRoomId() {
        final List<Map<String, String>> roomsMapList = new ArrayList<>();
        final Map<String, String> roomMap = new HashMap<>();
        roomMap.put("roomId", "");
        roomsMapList.add(roomMap);
        final OuterDomain outerDomain = new OuterDomain.OuterDomainBuilder()
                .addField("rooms", roomsMapList)
                .build();
        try {
            assertTrue(DomainDataUtil.getRoomIds(outerDomain).isEmpty());
        } catch (Exception e) {
           fail("This should not throw an exception");
        }        
    }


    @Test
    public void testOnlyRoomHero() {
        final List<Map<String, String>> roomsMapList = new ArrayList<>();
        final Map<String, String> roomMap = new HashMap<>();
        roomMap.put("roomHero", "false");
        roomsMapList.add(roomMap);
        final OuterDomain outerDomain = new OuterDomain.OuterDomainBuilder()
                .addField("rooms", roomsMapList)
                .build();
        assertTrue(DomainDataUtil.roomsFieldIsInvalid(outerDomain));
    }

    @Test
    public void testIntegerLCMId() throws Exception {
        Media dynamoMedia = new Media();
        dynamoMedia.setDomainFields("{\"category\":\"21001\",\"lcmMediaId\":16343312}");
        assertEquals("16343312", DomainDataUtil.getMediaIdFromDynamo(dynamoMedia));
    }

    @Test
    public void testStringLCMId() throws Exception {
        Media dynamoMedia = new Media();
        dynamoMedia.setDomainFields("{\"category\":\"21001\",\"lcmMediaId\":\"16343312\"}");
        assertEquals("16343312", DomainDataUtil.getMediaIdFromDynamo(dynamoMedia));
    }

    @Test
    public void testNullDomainFieldLCMId() throws Exception {
        Media dynamoMedia = new Media();
        dynamoMedia.setDomainFields(null);
        assertNull(DomainDataUtil.getMediaIdFromDynamo(dynamoMedia));
    }

    @Test
    public void testNullStringDomainFieldLCMId() throws Exception {
        Media dynamoMedia = new Media();
        dynamoMedia.setDomainFields("null");
        assertNull(DomainDataUtil.getMediaIdFromDynamo(dynamoMedia));
    }

    @Test
    public void testEmptyStringDomainFieldLCMId() throws Exception {
        Media dynamoMedia = new Media();
        dynamoMedia.setDomainFields("");
        assertNull(DomainDataUtil.getMediaIdFromDynamo(dynamoMedia));
    }

    @Test
    public void testNullLCMId() throws Exception {
        Media dynamoMedia = new Media();
        dynamoMedia.setDomainFields("{\"category\":\"21001\",\"lcmMediaId\":null}");
        assertNull(DomainDataUtil.getMediaIdFromDynamo(dynamoMedia));
    }

    @Test
    public void testNullStringLCMId() throws Exception {
        Media dynamoMedia = new Media();
        dynamoMedia.setDomainFields("{\"category\":\"21001\",\"lcmMediaId\":\"null\"}");
        assertNull(DomainDataUtil.getMediaIdFromDynamo(dynamoMedia));
    }

    @Test
    public void testEmptyStringLCMId() throws Exception {
        Media dynamoMedia = new Media();
        dynamoMedia.setDomainFields("{\"category\":\"21001\",\"lcmMediaId\":\"\"}");
        assertNull(DomainDataUtil.getMediaIdFromDynamo(dynamoMedia));
    }

}
