package com.expedia.content.media.processing.services.util;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

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
            assertFalse(DomainDataUtil.collectRoomIds(outerDomain).isEmpty());
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
    
    @SuppressWarnings("unchecked")
    @Test
    public void testValidateDomainFields() throws Exception{
        final ObjectMapper mapper = new ObjectMapper();
        String jsonMessage = "{  \n" +
                "   \"mediaGuid\":\"aaaaaaa-1010-bbbb-292929229\",\n" +
                "   \"mediaProviderId\":\"1001\",\n" +
                "   \"fileUrl\":\"http://images.com/dir1/img1.jpg\",\n" +
                "   \"stagingKey\":{  \n" +
                "      \"externalId\":\"222\",\n" +
                "      \"providerId\":\"300\",\n" +
                "      \"sourceId\":\"99\"\n" +
                "   },\n" +
                "   \"callback\":\"http:\\/\\/multi.source.callback\\/callback\",\n" +
                "   \"comment\":\"test comment!\",\n" +
                "   \"hidden\":\"true\",\n" +
                "   \"caption\":\"caption\",\n" +
                "   \"domain\": \"Lodging\",\n" +
                "   \"domainId\": \"1234\",\n" +
                "   \"domainFields\": {\n" +
                "      \"expediaId\":2001002,\n" +
                "      \"categoryIds\":[\"801\",\"304\"],\n" +
                "      \"moreFields\": {\n" +
                "         \"moreexpediaId\":11111\n" +
                "        }\n" +
                "     }\n" +
                "}";
    
    Map<String, Object> mapMessage = mapper.readValue(jsonMessage, Map.class);
    Object domainFields = mapMessage.get("domainFields");
    assertTrue(DomainDataUtil.domainFieldIsValid(domainFields));
    
    final String stringDomainFieldmessage ="{"
            +"\"userId\":\"PSG1Abakoglou\","
            +"\"active\":\"false\","
            +"\"domainFields\":\"null\","
            +"\"hidden\":null}";
    
    mapMessage = mapper.readValue(stringDomainFieldmessage, Map.class);
    domainFields = mapMessage.get("domainFields");
    assertFalse(DomainDataUtil.domainFieldIsValid(domainFields));

    final String nullDomainFieldmessage ="{"
            +"\"userId\":\"PSG1Abakoglou\","
            +"\"active\":\"false\","
            +"\"domainFields\":null,"
            +"\"hidden\":null}";
    mapMessage = mapper.readValue(nullDomainFieldmessage, Map.class);
    domainFields = mapMessage.get("domainFields");
    assertTrue(DomainDataUtil.domainFieldIsValid(domainFields));

    final String emptyMapDomainFieldmessage ="{"
            +"\"userId\":\"PSG1Abakoglou\","
            +"\"active\":\"false\","
            +"\"domainFields\":{},"
            +"\"hidden\":null}";
    mapMessage = mapper.readValue(emptyMapDomainFieldmessage, Map.class);
    domainFields = mapMessage.get("domainFields");
    assertTrue(DomainDataUtil.domainFieldIsValid(domainFields));
   }  
    
    @Test
    public void testCollectRoomIds() {
        Map<String, Object> room = new HashMap<>();
        List<Map<String, Object>> rooms = new ArrayList<>();
        room.put("roomId", "1673824");
        room.put("roomHero", Boolean.TRUE.toString());
        rooms.add(room);

        room = new HashMap<>();
        room.put("roomId", "not an integer");
        room.put("roomHero", Boolean.TRUE.toString());
        rooms.add(room);

        room = new HashMap<>();
        room.put("roomId", "");
        room.put("roomHero", Boolean.TRUE.toString());
        rooms.add(room);
        final OuterDomain outerDomain = new OuterDomain.OuterDomainBuilder()
                .addField("rooms", rooms)
                .build();
        List<Object> roomIds = DomainDataUtil.collectRoomIds(outerDomain);
        assertEquals(3, roomIds.size());
    }
    
    @Test
    public void testCollectMalFormatRoomIds() {
        Map<String, Object> room = new HashMap<>();
        List<Map<String, Object>> rooms = new ArrayList<>();
        room.put("roomId", "1673824");
        room.put("roomHero", Boolean.TRUE.toString());
        rooms.add(room);

        room = new HashMap<>();
        room.put("roomId", "not an integer");
        room.put("roomHero", Boolean.TRUE.toString());
        rooms.add(room);

        room = new HashMap<>();
        room.put("roomId", "");
        room.put("roomHero", Boolean.TRUE.toString());
        rooms.add(room);
        final OuterDomain outerDomain = new OuterDomain.OuterDomainBuilder()
                .addField("rooms", rooms)
                .build();
        List<Object> roomIds = DomainDataUtil.collectMalFormatRoomIds(outerDomain);
        assertEquals(2, roomIds.size());
        assertTrue(roomIds.get(0).toString().equals("not an integer"));
    }
    
    @Test
    public void testCollecValidFormatRoomIds() {
        Map<String, Object> room = new HashMap<>();
        List<Map<String, Object>> rooms = new ArrayList<>();
        room.put("roomId", "1673824");
        room.put("roomHero", Boolean.TRUE.toString());
        rooms.add(room);

        room = new HashMap<>();
        room.put("roomId", "not an integer");
        room.put("roomHero", Boolean.TRUE.toString());
        rooms.add(room);

        room = new HashMap<>();
        room.put("roomId", "");
        room.put("roomHero", Boolean.TRUE.toString());
        rooms.add(room);
        final OuterDomain outerDomain = new OuterDomain.OuterDomainBuilder()
                .addField("rooms", rooms)
                .build();
        List<Integer> roomIds = DomainDataUtil.collectValidFormatRoomIds(outerDomain);
        assertEquals(1, roomIds.size());
        assertEquals(1673824, roomIds.get(0).intValue());
    }    
}
