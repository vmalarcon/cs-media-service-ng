package com.expedia.content.media.processing.services.dao;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaDerivative;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaContentProviderNameGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaIdListSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaItemGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLRoomGetSproc;
import com.expedia.content.media.processing.services.util.ActivityMapping;

public class LcmDynamoMediaDaoTest {

    SQLRoomGetSproc roomGetSproc =null;

    @Before
    public void setUp() throws Exception {
        roomGetSproc = mock(SQLRoomGetSproc.class);
        Map<String, Object> roomResult = new HashMap<>();
        LcmMediaRoom lcmMediaRoom = LcmMediaRoom.builder().roomId(123).roomHero(true).build();
        List<LcmMediaRoom> lcmMediaRoomList = new ArrayList<>();
        lcmMediaRoomList.add(lcmMediaRoom);
        roomResult.put("room", lcmMediaRoomList);
        when(roomGetSproc.execute(anyInt())).thenReturn(roomResult);


    }
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetMediaByDomainIdNoFilter() throws Exception {
        SQLMediaIdListSproc mediaIdSproc = mock(SQLMediaIdListSproc.class);
        Integer mediaId1 = 1;
        Integer mediaId2 = 2;
        List<Integer> mediaIds = new ArrayList<>();
        mediaIds.add(mediaId1);
        mediaIds.add(mediaId2);
        Map<String, Object> idResult = new HashMap<>();
        idResult.put(SQLMediaIdListSproc.MEDIA_ID_SET, mediaIds);
        when(mediaIdSproc.execute(anyInt(), anyString())).thenReturn(idResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);
        List<LcmMedia> mediaList1 = new ArrayList<>();
        LcmMedia media1 = LcmMedia.builder().domainId(1234).mediaId(1).fileName("image1.jpg").active(true).width(20).height(21).fileSize(200)
                .lastUpdatedBy("test").lastUpdateDate(new Date()).provider(400).category(3).comment("Comment").formatId(2).build();
        mediaList1.add(media1);
        Map<String, Object> mediaResult1 = make2DerivativeMediaResult(mediaList1, 1, 1, 2, true, true, "image1_t.jpg", "image1_s.jpg");

        List<LcmMedia> mediaList2 = new ArrayList<>();
        LcmMedia media2 = LcmMedia.builder().domainId(1234).mediaId(2).fileName("image2.jpg").active(true).width(40).height(41).fileSize(500)
                .lastUpdatedBy("test").lastUpdateDate(new Date()).provider(400).category(500).comment("").build();
        mediaList2.add(media2);
        Map<String, Object> mediaResult2 = make2DerivativeMediaResult(mediaList2, 2, 1, 2, true, true, "image2_t.jpg", "image2_s.jpg");

        when(mediaSproc.execute(anyInt(), eq(mediaId1))).thenReturn(mediaResult1);
        when(mediaSproc.execute(anyInt(), eq(mediaId2))).thenReturn(mediaResult2);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder()
                .mediaGuid(guid1).domain("Lodging").domainId("1234").fileUrl("s3://fileUrl")
                .domainFields("{\"lcmMediaId\":\"1\",\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}")
                .derivatives("[{\"type\":\"v\",\"width\":179,\"height\":240,\"fileSize\":10622,\"location\":\"s3://ewe-cs-media-test/test/derivative/lodging/1000000/10000/7200/7139/dfec2df8_v.jpg\"}]")
                .build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").domainId("1234").build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaIdSproc, mediaSproc, mockMediaDBRepo, properties, null);
        setFieldValue(mediaDao, "roomGetSproc", roomGetSproc);
        List<Media> testMediaList = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, null);

        assertEquals(3, testMediaList.size());
        testMediaList.stream().filter(media -> media.getLcmMediaId() != null).forEach(media -> assertEquals(2, media.getDerivativesList().size()));
        Media testMedia1 = testMediaList.get(0);
        Map<String, Object> domainMap= testMedia1.getDomainData();
        List rooms = (ArrayList)domainMap.get("rooms");
        assertEquals(((HashMap)rooms.get(0)).get("roomId"),"123");
        assertTrue(Boolean.valueOf(((HashMap) rooms.get(0)).get("roomHero").toString()));

        assertEquals(media1.getDomainId().toString(), testMedia1.getDomainId());
        assertEquals(media1.getLastUpdatedBy(), testMedia1.getUserId());
        assertEquals(media1.getFileName(), testMedia1.getFileName());
        assertEquals(dynamoMedia1.getMediaGuid(), testMedia1.getMediaGuid());
        assertEquals(media1.getComment(), testMedia1.getCommentList().get(0));
        assertTrue((media1.getFileSize() * 1024L) == testMedia1.getFileSize());
        assertEquals("true", testMedia1.getDomainData().get("propertyHero"));
        assertEquals("4321", testMedia1.getDomainData().get("subcategoryId"));
        assertEquals("VirtualTour", testMedia1.getDomainDerivativeCategory());
        assertEquals("s3://fileUrl", testMedia1.getFileUrl());
        Media testMedia2 = testMediaList.get(1);
        assertNull(testMedia2.getMediaGuid());
        assertNull(testMedia2.getCommentList());
        assertNull(testMedia2.getFileUrl());
        Media testMedia3 = testMediaList.get(2);
        assertEquals(dynamoMedia3.getMediaGuid(), testMedia3.getMediaGuid());
        assertNull(testMedia3.getFileUrl());
    }

    @Test
    public void testGetMediaByDomainIdCallSprocMultiple() throws Exception {
        SQLMediaIdListSproc mediaIdSproc = mock(SQLMediaIdListSproc.class);
        Integer mediaId1 = 1;
        Integer mediaId2 = 2;
        Integer mediaId3 = 3;
        List<Integer> mediaIds = new ArrayList<>();
        mediaIds.add(mediaId1);
        mediaIds.add(mediaId2);
        mediaIds.add(mediaId3);

        Map<String, Object> idResult = new HashMap<>();
        idResult.put(SQLMediaIdListSproc.MEDIA_ID_SET, mediaIds);
        when(mediaIdSproc.execute(anyInt(), anyString())).thenReturn(idResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);
        List<LcmMedia> mediaList1 = new ArrayList<>();
        LcmMedia media1 = LcmMedia.builder().domainId(1234).mediaId(1).fileName("image1.jpg").active(true).width(20).height(21).fileSize(200)
                .lastUpdatedBy("test").lastUpdateDate(new Date()).provider(400).category(3).comment("Comment").formatId(2).build();
        mediaList1.add(media1);
        Map<String, Object> mediaResult1 = make2DerivativeMediaResult(mediaList1, 1, 1, 2, true, true, "image1_t.jpg", "image1_s.jpg");

        List<LcmMedia> mediaList2 = new ArrayList<>();
        LcmMedia media2 = LcmMedia.builder().domainId(1234).mediaId(2).fileName("image2.jpg").active(true).width(40).height(41).fileSize(500)
                .lastUpdatedBy("test").lastUpdateDate(new Date()).provider(400).category(500).comment("").build();
        mediaList2.add(media2);
        Map<String, Object> mediaResult2 = make2DerivativeMediaResult(mediaList2, 2, 1, 2, true, true, "image2_t.jpg", "image2_s.jpg");

        List<LcmMedia> mediaList3 = new ArrayList<>();
        LcmMedia media3 = LcmMedia.builder().domainId(1234).mediaId(3).fileName("image3.jpg").active(true).width(40).height(41).fileSize(500)
                .lastUpdatedBy("test").lastUpdateDate(new Date()).provider(400).category(500).comment("").build();
        mediaList3.add(media3);
        Map<String, Object> mediaResult3 = make2DerivativeMediaResult(mediaList2, 2, 1, 2, true, true, "image3_t.jpg", "image3_s.jpg");

        when(mediaSproc.execute(anyInt(), eq(mediaId1))).thenReturn(mediaResult1);
        when(mediaSproc.execute(anyInt(), eq(mediaId2))).thenReturn(mediaResult2);
        when(mediaSproc.execute(anyInt(), eq(mediaId3))).thenReturn(mediaResult3);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "imspecial-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder().mediaGuid(guid1).lcmMediaId("1").domain("Lodging").domainId("1234")
                .domainFields("{\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}").build();
        String guid2 = "f2d4d480-9627-47f9-86c6-1874c18d3bbc";
        Media dynamoMedia2 = Media.builder().mediaGuid(guid2).domain("Lodging").domainId("1234").build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").fileName("image3.jpg").domainId("1234").build();
        dynamoMediaList.add(dynamoMedia2);
        dynamoMediaList.add(dynamoMedia3);
        dynamoMediaList.add(dynamoMedia1); //Put hero last to verify it returns as first item.
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaIdSproc, mediaSproc, mockMediaDBRepo, properties, null);
        LcmProcessLogDao lcmProcessLogDao = makeMockProcessLogDao();
        setFieldValue(mediaDao, "processLogDao", lcmProcessLogDao);
        setFieldValue(mediaDao, "paramLimit", 2);
        setFieldValue(mediaDao, "roomGetSproc", roomGetSproc);

        List<Media> testMediaList = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, null);
        verify(lcmProcessLogDao, times(2)).findMediaStatus(any());
        assertEquals(guid1, testMediaList.get(0).getMediaGuid());
        assertEquals("true", testMediaList.get(0).getDomainData().get("propertyHero"));
    }

    @Test
    public void testGetContentProviderName() throws Exception {
        SQLMediaContentProviderNameGetSproc mockMediaContentProviderNameGetSproc =mock(SQLMediaContentProviderNameGetSproc.class);
        LcmMedia lcmMedia = LcmMedia.builder().fileName("4600417_IMG0010.jpg").domainId(4600417).filProcessedBool(true).build();
        List<LcmMedia> lcmMedias = new ArrayList<>();
        lcmMedias.add(lcmMedia);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaContentProviderNameGetSproc.MEDIA_ATTRS,lcmMedias);

        when(mockMediaContentProviderNameGetSproc.execute(anyString())).thenReturn(mediaResult);
        MediaDao mediaDao = new LcmDynamoMediaDao();
        setFieldValue(mediaDao, "mediaContentProviderNameGetSproc", mockMediaContentProviderNameGetSproc);

        LcmMedia lcmMediaResult = mediaDao.getContentProviderName("test.jpg");
        assertEquals("4600417_IMG0010.jpg", lcmMediaResult.getFileName());
        assertTrue(4600417 ==lcmMediaResult.getDomainId());


    }

    @Test
    public void testFilterActive() throws Exception {
        SQLMediaIdListSproc mediaIdSproc = mock(SQLMediaIdListSproc.class);
        Integer mediaId1 = 1;
        Integer mediaId2 = 2;
        List<Integer> mediaIds = new ArrayList<>();
        mediaIds.add(mediaId1);
        mediaIds.add(mediaId2);
        Map<String, Object> idResult = new HashMap<>();
        idResult.put(SQLMediaIdListSproc.MEDIA_ID_SET, mediaIds);
        when(mediaIdSproc.execute(anyInt(), anyString())).thenReturn(idResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);
        LcmMedia media1 = LcmMedia.builder().domainId(1234).mediaId(1).fileName("image1.jpg").active(true).width(20).height(21).fileSize(200)
                .lastUpdatedBy("test").lastUpdateDate(new Date()).provider(400).category(3).comment("Comment").build();
        List<LcmMedia> mediaList1 = new ArrayList<>();
        mediaList1.add(media1);
        Map<String, Object> mediaResult1 = make2DerivativeMediaResult(mediaList1, 1, 1, 2, true, true, "image1_t.jpg", "image1_s.jpg");

        LcmMedia media2 = LcmMedia.builder().domainId(1234).mediaId(2).fileName("image2.jpg").active(false).width(40).height(41).fileSize(500)
                .lastUpdatedBy("test").lastUpdateDate(new Date()).provider(400).category(500).comment("").build();
        List<LcmMedia> mediaList2 = new ArrayList<>();
        mediaList2.add(media2);
        Map<String, Object> mediaResult2 = make2DerivativeMediaResult(mediaList2, 2, 1, 2, true, true, "image2_t.jpg", "image2_s.jpg");

        when(mediaSproc.execute(anyInt(), eq(mediaId1))).thenReturn(mediaResult1);
        when(mediaSproc.execute(anyInt(), eq(mediaId2))).thenReturn(mediaResult2);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder().mediaGuid(guid1).lcmMediaId("1").domain("Lodging").domainId("1234")
                .domainFields("{\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}").build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").domainId("1234").build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaIdSproc, mediaSproc, mockMediaDBRepo, properties, null);
        setFieldValue(mediaDao, "lcmMediaIdSproc", mediaIdSproc);
        setFieldValue(mediaDao, "lcmMediaItemSproc", mediaSproc);
        setFieldValue(mediaDao, "roomGetSproc", roomGetSproc);
        List<Media> testMediaList1 = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", "true", null);

        assertEquals(1, testMediaList1.size());
        testMediaList1.stream().forEach(media -> assertEquals(2, media.getDerivativesList().size()));
        Media testMedia1 = testMediaList1.get(0);
        assertEquals(media1.getDomainId().toString(), testMedia1.getDomainId());
        assertEquals(media1.getLastUpdatedBy(), testMedia1.getUserId());
        assertEquals(media1.getFileName(), testMedia1.getFileName());
        assertEquals(dynamoMedia1.getMediaGuid(), testMedia1.getMediaGuid());
        assertTrue((media1.getFileSize() * 1024L) == testMedia1.getFileSize());

        List<Media> testMediaList2 = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", "false", null);

        assertEquals(2, testMediaList2.size());
        testMediaList2.stream().filter(media -> media.getLcmMediaId() != null).forEach(media -> assertEquals(2, media.getDerivativesList().size()));
        Media testMedia2 = testMediaList2.get(0);
        assertEquals(media2.getDomainId().toString(), testMedia2.getDomainId());
        assertEquals(media2.getLastUpdatedBy(), testMedia2.getUserId());
        assertEquals(media2.getFileName(), testMedia2.getFileName());
        assertTrue((media2.getFileSize() * 1024L) == testMedia2.getFileSize());

    }

    @Test
    public void testFilterDerivatives() throws Exception {
        SQLMediaIdListSproc mediaIdSproc = mock(SQLMediaIdListSproc.class);
        Integer mediaId1 = 1;
        Integer mediaId2 = 2;
        List<Integer> mediaIds = new ArrayList<>();
        mediaIds.add(mediaId1);
        mediaIds.add(mediaId2);
        Map<String, Object> idResult = new HashMap<>();
        idResult.put(SQLMediaIdListSproc.MEDIA_ID_SET, mediaIds);
        when(mediaIdSproc.execute(anyInt(), anyString())).thenReturn(idResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);
        LcmMedia media1 = LcmMedia.builder().domainId(1234).mediaId(1).fileName("image1.jpg").active(true).width(20).height(21).fileSize(200)
                .lastUpdatedBy("test").lastUpdateDate(new Date()).provider(400).category(3).comment("Comment").build();
        List<LcmMedia> mediaList1 = new ArrayList<>();
        mediaList1.add(media1);
        Map<String, Object> mediaResult1 = make2DerivativeMediaResult(mediaList1, 1, 1, 2, true, true, "image1_t.jpg", "image1_s.jpg");

        LcmMedia media2 = LcmMedia.builder().domainId(1234).mediaId(2).fileName("image2.jpg").active(true).width(40).height(41).fileSize(500)
                .lastUpdatedBy("test").lastUpdateDate(new Date()).provider(400).category(500).comment("").build();
        List<LcmMedia> mediaList2 = new ArrayList<>();
        mediaList2.add(media2);
        Map<String, Object> mediaResult2 = make2DerivativeMediaResult(mediaList2, 2, 1, 2, true, true, "image2_t.jpg", "image2_s.jpg");

        when(mediaSproc.execute(anyInt(), eq(mediaId1))).thenReturn(mediaResult1);
        when(mediaSproc.execute(anyInt(), eq(mediaId2))).thenReturn(mediaResult2);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder().mediaGuid(guid1).lcmMediaId("1").domain("Lodging").domainId("1234")
                .domainFields("{\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}").build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").domainId("1234").build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaIdSproc, mediaSproc, mockMediaDBRepo, properties, null);
        setFieldValue(mediaDao, "lcmMediaIdSproc", mediaIdSproc);
        setFieldValue(mediaDao, "lcmMediaItemSproc", mediaSproc);
        setFieldValue(mediaDao, "roomGetSproc", roomGetSproc);
        List<Media> testMediaList1 = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, "a,t,b");

        assertEquals(3, testMediaList1.size());
        testMediaList1.stream().filter(media -> media.getLcmMediaId() != null).forEach(media -> {
            assertEquals(1, media.getDerivativesList().size());
            assertTrue(media.getDerivativesList().get(0).get("type").equals("t"));
        });
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetMediaByDomainIdFileProcessFalse() throws Exception {
        SQLMediaIdListSproc mediaIdSproc = mock(SQLMediaIdListSproc.class);
        Integer mediaId1 = 1;
        Integer mediaId2 = 2;
        List<Integer> mediaIds = new ArrayList<>();
        mediaIds.add(mediaId1);
        mediaIds.add(mediaId2);
        Map<String, Object> idResult = new HashMap<>();
        idResult.put(SQLMediaIdListSproc.MEDIA_ID_SET, mediaIds);
        when(mediaIdSproc.execute(anyInt(), anyString())).thenReturn(idResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);
        List<LcmMedia> mediaList1 = new ArrayList<>();
        LcmMedia media1 = LcmMedia.builder().domainId(1234).mediaId(1).fileName("image1.jpg").active(true).width(20).height(21).fileSize(200)
                .lastUpdatedBy("test").lastUpdateDate(new Date()).provider(400).category(3).comment("Comment").formatId(2).build();
        mediaList1.add(media1);
        Map<String, Object> mediaResult1 = make2DerivativeMediaResult(mediaList1, 1, 1, 2, false, false, "image1_t.jpg", "image1_s.jpg");

        List<LcmMedia> mediaList2 = new ArrayList<>();
        LcmMedia media2 = LcmMedia.builder().domainId(1234).mediaId(2).fileName("image2.jpg").active(true).width(40).height(41).fileSize(500)
                .lastUpdatedBy("test").lastUpdateDate(new Date()).provider(400).category(500).comment("").build();
        mediaList2.add(media2);
        Map<String, Object> mediaResult2 = make2DerivativeMediaResult(mediaList2, 2, 1, 2, false, true, "image2_t.jpg", "image2_s.jpg");

        when(mediaSproc.execute(anyInt(), eq(mediaId1))).thenReturn(mediaResult1);
        when(mediaSproc.execute(anyInt(), eq(mediaId2))).thenReturn(mediaResult2);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder().mediaGuid(guid1).lcmMediaId("1").domain("Lodging").domainId("1234")
                .domainFields("{\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}").build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").domainId("1234").build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaIdSproc, mediaSproc, mockMediaDBRepo, properties, null);
        setFieldValue(mediaDao, "roomGetSproc", roomGetSproc);
        List<Media> testMediaList = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, null);

        assertEquals(3, testMediaList.size());
        Media testMedia1 = testMediaList.get(0);
        assertEquals(media1.getDomainId().toString(), testMedia1.getDomainId());
        assertEquals(media1.getLastUpdatedBy(), testMedia1.getUserId());
        assertEquals(media1.getFileName(), testMedia1.getFileName());
        assertEquals(dynamoMedia1.getMediaGuid(), testMedia1.getMediaGuid());
        assertEquals(media1.getComment(), testMedia1.getCommentList().get(0));
        assertTrue((media1.getFileSize() * 1024L) == testMedia1.getFileSize());
        assertEquals(0, testMedia1.getDerivativesList().size());
        assertEquals("true", testMedia1.getDomainData().get("propertyHero"));
        assertEquals("4321", testMedia1.getDomainData().get("subcategoryId"));
        assertEquals("VirtualTour", testMedia1.getDomainDerivativeCategory());
        Media testMedia2 = testMediaList.get(1);
        assertNull(testMedia2.getMediaGuid());
        assertNull(testMedia2.getCommentList());
        assertEquals(1, testMedia2.getDerivativesList().size());
        HashMap derivative = (HashMap) testMedia2.getDerivativesList().get(0);
        assertEquals(derivative.get("fileSize"), 200 * 1024L);
        assertEquals(derivative.get("width"), 20);
        assertEquals(derivative.get("height"), 21);
        assertEquals(derivative.get("type"), "s");
        assertEquals(derivative.get("location"), "https://media.int.expedia.com/image2_s.jpg");
        Media testMedia3 = testMediaList.get(2);
        assertEquals(dynamoMedia3.getMediaGuid(), testMedia3.getMediaGuid());
        assertNull(testMedia3.getDerivativesList());
    }
    
    @Test
    public void testMediaGetNotFoundGUID() throws NoSuchFieldException, IllegalAccessException {
        MediaDao mediaDao = makeMockMediaDao(null, null, mock(DynamoMediaRepository.class), null, null);
        assertNull(mediaDao.getMediaByGUID("d2d4d480-9627-47f9-86c6-1874c18d34t6"));
    }

    @Test
    public void testMediaGetNotFoundLCMMediaId() throws NoSuchFieldException, IllegalAccessException {
        SQLMediaGetSproc mockMediaGetSproc = mock(SQLMediaGetSproc.class);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(SQLMediaGetSproc.MEDIA_SET, new ArrayList<LcmMedia>());
        when(mockMediaGetSproc.execute(any(Integer.class))).thenReturn(resultMap);
        MediaDao mediaDao = makeMockMediaDao(null, null, mock(DynamoMediaRepository.class), null, mockMediaGetSproc);
        assertNull(mediaDao.getMediaByGUID("4321"));
    }

    @Test
    public void testMediaGetGUIDNoLcm() throws NoSuchFieldException, IllegalAccessException {
        final Properties mockProperties = new Properties();
        mockProperties.put("1", "EPC Internal User");

        String guid = "d2d4d480-9627-47f9-86c6-1874c18d34t6";
        Media guidMedia = Media.builder().active("true").mediaGuid(guid).fileName("super_potato.jpg").build();
        DynamoMediaRepository mediaDynamo = mock(DynamoMediaRepository.class);
        when(mediaDynamo.getMedia(guid)).thenReturn(guidMedia);
        
        MediaDao mediaDao = makeMockMediaDao(null, null, mediaDynamo, mockProperties, null);
        Media resultMedia = mediaDao.getMediaByGUID(guid);
        assertEquals(guidMedia, resultMedia);
    }

    @Test
    public void testMediaGetLcmNoGuid() throws NoSuchFieldException, IllegalAccessException {
        final Properties mockProperties = new Properties();
        mockProperties.put("1", "EPC Internal User");

        Integer lcmMediaId = 54321;
        Integer domainId = 12345;
        
        LcmMedia mediaGetMedia = LcmMedia.builder().active(true).domainId(domainId).mediaId(lcmMediaId).fileName("super_potato.jpg").formatId(1).build();
        Map<String, Object> getResultMap = new HashMap<>();
        List<LcmMedia> mediaGetList = new ArrayList<>();
        mediaGetList.add(mediaGetMedia);
        getResultMap.put(SQLMediaGetSproc.MEDIA_SET, mediaGetList);
        SQLMediaGetSproc mockMediaGetSproc = mock(SQLMediaGetSproc.class);
        when(mockMediaGetSproc.execute(any(Integer.class))).thenReturn(getResultMap);
        
        LcmMedia mediaItemMedia =
                LcmMedia.builder().domainId(domainId).mediaId(lcmMediaId).provider(1).active(true).fileName("super_potato.jpg").width(4000).height(2000)
                        .lastUpdatedBy("bob").fileSize(42424242).lastUpdateDate(new Date()).category(45).comment("hello").formatId(1).build();
        Map<String, Object> itemResultMap = new HashMap<>();
        List<LcmMedia> mediaItemList = new ArrayList<>();
        mediaItemList.add(mediaItemMedia);
        itemResultMap.put(SQLMediaItemGetSproc.MEDIA_SET, mediaItemList);
        List<LcmMediaDerivative> mediaItemDerivativeList = new ArrayList<>();
        itemResultMap.put(SQLMediaItemGetSproc.MEDIA_DERIVATIVES_SET, mediaItemDerivativeList);
        SQLMediaItemGetSproc mockMediaItemSproc = mock(SQLMediaItemGetSproc.class);
        when(mockMediaItemSproc.execute(eq(domainId), eq(lcmMediaId))).thenReturn(itemResultMap);
        
        MediaDao mediaDao = makeMockMediaDao(null, mockMediaItemSproc, mock(DynamoMediaRepository.class), mockProperties, mockMediaGetSproc);
        setFieldValue(mediaDao, "roomGetSproc", roomGetSproc);
        Media resultMedia = mediaDao.getMediaByGUID(lcmMediaId.toString());
        assertEquals(lcmMediaId.toString(), resultMedia.getLcmMediaId());
    }

    @Test
    public void testMediaGetGuidAndLcm() throws NoSuchFieldException, IllegalAccessException {
        final Properties mockProperties = new Properties();
        mockProperties.put("1", "EPC Internal User");

        Integer lcmMediaId = 54321;
        Integer domainId = 12345;
        
        String guid = "d2d4d480-9627-47f9-86c6-1874c18d34t6";
        Media guidMedia = Media.builder().active("true").mediaGuid(guid).domain(Domain.LODGING.getDomain()).domainId(domainId.toString())
                .lcmMediaId(lcmMediaId.toString()).fileName("super_potato.jpg").build();
        DynamoMediaRepository mediaDynamo = mock(DynamoMediaRepository.class);
        when(mediaDynamo.getMedia(guid)).thenReturn(guidMedia);

        LcmMedia mediaItemMedia =
                LcmMedia.builder().domainId(domainId).mediaId(lcmMediaId).provider(1).active(true).fileName("super_potato.jpg").width(4000).height(2000)
                        .lastUpdatedBy("bob").fileSize(42424242).lastUpdateDate(new Date()).category(45).comment("hello").formatId(1).build();
        Map<String, Object> itemResultMap = new HashMap<>();
        List<LcmMedia> mediaItemList = new ArrayList<>();
        mediaItemList.add(mediaItemMedia);
        itemResultMap.put(SQLMediaItemGetSproc.MEDIA_SET, mediaItemList);
        List<LcmMediaDerivative> mediaItemDerivativeList = new ArrayList<>();
        itemResultMap.put(SQLMediaItemGetSproc.MEDIA_DERIVATIVES_SET, mediaItemDerivativeList);
        SQLMediaItemGetSproc mockMediaItemSproc = mock(SQLMediaItemGetSproc.class);
        when(mockMediaItemSproc.execute(eq(domainId), eq(lcmMediaId))).thenReturn(itemResultMap);
        
        MediaDao mediaDao = makeMockMediaDao(null, mockMediaItemSproc, mediaDynamo, mockProperties, null);
        setFieldValue(mediaDao, "roomGetSproc", roomGetSproc);
        Media resultMedia = mediaDao.getMediaByGUID(guid);
        assertEquals(lcmMediaId.toString(), resultMedia.getLcmMediaId());
    }
    
    private MediaDao makeMockMediaDao(SQLMediaIdListSproc mediaIdSproc, SQLMediaItemGetSproc mediaItemSproc, DynamoMediaRepository mockMediaDBRepo,
                                      final Properties properties, SQLMediaGetSproc mediaGetSproc) throws NoSuchFieldException, IllegalAccessException {
        MediaDao mediaDao = new LcmDynamoMediaDao();
        setFieldValue(mediaDao, "lcmMediaIdSproc", mediaIdSproc);
        setFieldValue(mediaDao, "lcmMediaSproc", mediaGetSproc);
        setFieldValue(mediaDao, "lcmMediaItemSproc", mediaItemSproc);
        setFieldValue(mediaDao, "paramLimit", 50);

        setFieldValue(mediaDao, "mediaRepo", mockMediaDBRepo);
        setFieldValue(mediaDao, "providerProperties", properties);
        setFieldValue(mediaDao, "processLogDao", makeMockProcessLogDao());
        setFieldValue(mediaDao, "activityWhiteList", makeActivityWhitelist());
        setFieldValue(mediaDao, "imageRootPath", "https://media.int.expedia.com/");
        return mediaDao;
    }

    private List<ActivityMapping> makeActivityWhitelist() {
        List<ActivityMapping> whitelist = new ArrayList<>();
        ActivityMapping activityMapping = new ActivityMapping();
        activityMapping.setActivityType("Publish");
        activityMapping.setMediaType(".*");
        activityMapping.setStatusMessage("PUBLISHED");
        whitelist.add(activityMapping);
        return whitelist;
    }

    private Map<String, Object> make2DerivativeMediaResult(List<LcmMedia> mediaList1, int mediaId, int type1, int type2, boolean published1,
                                                           boolean published2, String fileName1, String fileName2) {
        Map<String, Object> mediaResult1 = new HashMap<>();
        mediaResult1.put(SQLMediaItemGetSproc.MEDIA_SET, mediaList1);
        List<LcmMediaDerivative> derivativeList1 = new ArrayList<>();
        LcmMediaDerivative derivative11 = LcmMediaDerivative.builder().mediaId(mediaId).fileProcessed(published1).mediaSizeTypeId(type1).fileName(fileName1)
                .width(10).height(11).fileSize(100).build();
        derivativeList1.add(derivative11);
        LcmMediaDerivative derivative12 = LcmMediaDerivative.builder().mediaId(mediaId).fileProcessed(published2).mediaSizeTypeId(type2).fileName(fileName2)
                .width(20).height(21).fileSize(200).build();
        derivativeList1.add(derivative12);
        mediaResult1.put(SQLMediaItemGetSproc.MEDIA_DERIVATIVES_SET, derivativeList1);
        return mediaResult1;
    }

    private LcmProcessLogDao makeMockProcessLogDao() {
        LcmProcessLogDao mockProcessLogDao = mock(LcmProcessLogDao.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<MediaProcessLog>();
        MediaProcessLog mediaLogStatus1 = new MediaProcessLog("2014-07-29 10:08:11.6890000 -07:00", "image1.jpg", "Something", "Lodging");
        mediaLogStatuses.add(mediaLogStatus1);
        MediaProcessLog mediaLogStatus2 = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "image1.jpg", "Publish", "Lodging");
        mediaLogStatuses.add(mediaLogStatus2);
        when(mockProcessLogDao.findMediaStatus(any())).thenReturn(mediaLogStatuses);
        return mockProcessLogDao;
    }

}
