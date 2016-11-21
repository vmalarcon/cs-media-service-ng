package com.expedia.content.media.processing.services.dao;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaAndDerivative;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaDerivative;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaContentProviderNameGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaDeleteSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaItemGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaListSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLRoomGetByCatalogItemIdSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLRoomGetByMediaIdSproc;
import com.expedia.content.media.processing.services.reqres.Comment;
import com.expedia.content.media.processing.services.reqres.DomainIdMedia;
import com.expedia.content.media.processing.services.reqres.MediaByDomainIdResponse;
import com.expedia.content.media.processing.services.reqres.MediaGetResponse;
import com.expedia.content.media.processing.services.util.ActivityMapping;
import com.expedia.content.media.processing.services.util.FileNameUtil;
import com.google.common.collect.Maps;

public class LcmDynamoMediaDaoTest {

    private static final String RESPONSE_FIELD_LCM_MEDIA_ID = "lcmMediaId";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS ZZ");

    private SQLRoomGetByMediaIdSproc roomGetByMediaIdSproc = null;
    private SQLRoomGetByCatalogItemIdSproc roomGetByCatalogItemIdSproc = null;

    @Before
    public void setUp() throws Exception {
        roomGetByMediaIdSproc = mock(SQLRoomGetByMediaIdSproc.class);
        Map<String, Object> roomByMediaIdResult = new HashMap<>();
        LcmMediaRoom lcmMediaRoom = LcmMediaRoom.builder().mediaId(1).roomId(123).roomHero(true).build();
        List<LcmMediaRoom> lcmMediaRoomByMediaIdList = new ArrayList<>();
        lcmMediaRoomByMediaIdList.add(lcmMediaRoom);
        roomByMediaIdResult.put("room", lcmMediaRoomByMediaIdList);
        when(roomGetByMediaIdSproc.execute(anyInt())).thenReturn(roomByMediaIdResult);


        roomGetByCatalogItemIdSproc = mock(SQLRoomGetByCatalogItemIdSproc.class);
        Map<String, Object> roomByCatalogItemIdResult = new HashMap<>();
        lcmMediaRoom = LcmMediaRoom.builder().mediaId(1).roomId(123).roomHero(true).build();
        List<LcmMediaRoom> lcmMediaRoomByCatalogItemIdList = new ArrayList<>();
        lcmMediaRoomByCatalogItemIdList.add(lcmMediaRoom);
        roomByCatalogItemIdResult.put("room", lcmMediaRoomByCatalogItemIdList);
        when(roomGetByCatalogItemIdSproc.execute(anyInt())).thenReturn(roomByCatalogItemIdResult);

    }
    @SuppressWarnings("rawtypes")
    @Test
    public void testGetMediaByDomainIdNoFilter() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, false, true, true, true, true, 2);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder()
                .mediaGuid(guid1).domain("Lodging").domainId("1234").fileUrl("s3://fileUrl/imageName.jpg")
                .domainFields("{\"lcmMediaId\":\"1\",\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}")
                .derivatives("[{\"type\":\"v\",\"width\":179,\"height\":240,\"fileSize\":10622,\"location\":\"s3://ewe-cs-media-test/test/derivative/lodging/1000000/10000/7200/7139/dfec2df8_v.jpg\"}]")
                .lastUpdated(new Date())
                .build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").domainId("1234").lastUpdated(LocalDateTime.now().minusMinutes(5).toDate()).build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");


        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        MediaByDomainIdResponse testMediaRespomse = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, null, null, null, null);

        assertEquals(3, testMediaRespomse.getImages().size());
        testMediaRespomse.getImages().stream().filter(media -> media.getDomainFields() != null && media.getDomainFields().get("lcmMediaId") != null).forEach(media -> assertEquals(2, media.getDerivatives().size()));
        DomainIdMedia testMedia1 = testMediaRespomse.getImages().get(0);
        Map<String, Object> domainMap = testMedia1.getDomainFields();
        List rooms = (ArrayList)domainMap.get("rooms");
        assertEquals(((HashMap)rooms.get(0)).get("roomId"),"123");
        assertTrue(Boolean.valueOf(((HashMap) rooms.get(0)).get("roomHero").toString()));

        assertEquals(mediaList.get(0).getDomainId().toString(), testMediaRespomse.getDomainId());
        assertEquals(mediaList.get(0).getLastUpdatedBy(), testMedia1.getLastUpdatedBy());
        assertEquals("imageName.jpg", testMedia1.getFileName());
        assertEquals(dynamoMedia1.getMediaGuid(), testMedia1.getMediaGuid());
        assertEquals(mediaList.get(0).getComment(), testMedia1.getComments().get(0).getNote());
        assertTrue((mediaList.get(0).getFileSize() * 1024L) == testMedia1.getFileSize());
        assertEquals("true", testMedia1.getDomainFields().get("propertyHero"));
        assertEquals("4321", testMedia1.getDomainFields().get("subcategoryId"));
        assertEquals("VirtualTour", testMedia1.getDomainDerivativeCategory());
        assertEquals("s3://fileUrl/imageName.jpg", testMedia1.getFileUrl());
        DomainIdMedia testMedia2 = testMediaRespomse.getImages().get(2);
        assertNull(testMedia2.getMediaGuid());
        assertNull(testMedia2.getFileUrl());
        DomainIdMedia testMedia3 = testMediaRespomse.getImages().get(1);
        assertEquals(dynamoMedia3.getMediaGuid(), testMedia3.getMediaGuid());
        assertNull(testMedia3.getFileUrl());
    }

    @Test
    public void testGetMediaByDomainIdCallSprocMultiple() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, true, true, true, true, true, null);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        LcmMediaAndDerivative media3d1 = LcmMediaAndDerivative.builder().mediaId(3).domainId(1234).fileName("image3.jpg").active(true).width(40)
                .height(41).fileSize(500).lastUpdatedBy("test").lastUpdateDate(new Date()).mediaLastUpdatedBy("test").mediaLastUpdateDate(new Date())
                .provider(400).category(500).comment("Comment").fileProcessed(true).derivativeSizeTypeId(1).derivativeFileName("image3_t.jpg")
                .derivativeWidth(10).derivativeHeight(11).derivativeFileSize(100).build();
        LcmMediaAndDerivative media3d2 = LcmMediaAndDerivative.builder().mediaId(3).domainId(1234).fileName("image3.jpg").active(true).width(40)
                .height(41).fileSize(500).lastUpdatedBy("test").lastUpdateDate(new Date()).mediaLastUpdatedBy("test").mediaLastUpdateDate(new Date())
                .provider(400).category(500).comment("Comment").fileProcessed(true).derivativeSizeTypeId(2).derivativeFileName("image3_s.jpg")
                .derivativeWidth(20).derivativeHeight(21).derivativeFileSize(200).build();
        mediaList.add(media3d1);
        mediaList.add(media3d2);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "imspecial-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder().mediaGuid(guid1).lcmMediaId("1").domain("Lodging").domainId("1234").lastUpdated(new Date())
                .domainFields("{\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}").build();
        String guid2 = "f2d4d480-9627-47f9-86c6-1874c18d3bbc";
        Media dynamoMedia2 = Media.builder().mediaGuid(guid2).domain("Lodging").domainId("1234").lastUpdated(new Date()).build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").fileName("image3.jpg").domainId("1234").lastUpdated(new Date()).build();
        dynamoMediaList.add(dynamoMedia2);
        dynamoMediaList.add(dynamoMedia3);
        dynamoMediaList.add(dynamoMedia1); //Put hero last to verify it returns as first item.
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        LcmProcessLogDao lcmProcessLogDao = makeMockProcessLogDao();
        setFieldValue(mediaDao, "processLogDao", lcmProcessLogDao);
        setFieldValue(mediaDao, "paramLimit", 2);

        List<DomainIdMedia> testMediaList = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, null, null, null, null).getImages();
        verify(lcmProcessLogDao, times(2)).findMediaStatus(any());
        assertEquals(guid1, testMediaList.get(0).getMediaGuid());
        assertEquals("true", testMediaList.get(0).getDomainFields().get("propertyHero"));
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
    public void testGetContentProviderNameWithInActive() throws Exception {
        SQLMediaContentProviderNameGetSproc mockMediaContentProviderNameGetSproc =mock(SQLMediaContentProviderNameGetSproc.class);
        LcmMedia lcmMedia = LcmMedia.builder().fileName("4600417_IMG0010.jpg").domainId(4600417).filProcessedBool(false).build();
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
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, false, true, true, true, true, null);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder().mediaGuid(guid1).lcmMediaId("1").domain("Lodging").domainId("1234").lastUpdated(new Date())
                .domainFields("{\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}").build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").domainId("1234")
                .lastUpdated(LocalDateTime.now().minusMinutes(5).toDate()).build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        MediaByDomainIdResponse testMediaResponse1 = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", "true", null, null, null, null);

        assertEquals(1, testMediaResponse1.getImages().size());
        testMediaResponse1.getImages()
                .forEach(media -> assertEquals(2, media.getDerivatives().size()));
        DomainIdMedia testMedia1 = testMediaResponse1.getImages().get(0);
        assertEquals(mediaList.get(0).getDomainId().toString(), testMediaResponse1.getDomainId());
        assertEquals(mediaList.get(0).getLastUpdatedBy(), testMedia1.getLastUpdatedBy());
        assertEquals(mediaList.get(0).getFileName(), testMedia1.getFileName());
        assertEquals(dynamoMedia1.getMediaGuid(), testMedia1.getMediaGuid());
        assertTrue((mediaList.get(0).getFileSize() * 1024L) == testMedia1.getFileSize());

        MediaByDomainIdResponse testMediaResponse2 = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", "false", null, null, null, null);

        assertEquals(2, testMediaResponse2.getImages().size());
        testMediaResponse2.getImages().stream().filter(media -> media.getDomainFields() != null && media.getDomainFields().get("lcmMediaId") != null)
                .forEach(media -> assertEquals(2, media.getDerivatives().size()));
        DomainIdMedia testMedia2 = testMediaResponse2.getImages().get(1);
        assertEquals(mediaList.get(2).getDomainId().toString(), testMediaResponse2.getDomainId());
        assertEquals(mediaList.get(2).getLastUpdatedBy(), testMedia2.getLastUpdatedBy());
        assertEquals(mediaList.get(2).getFileName(), testMedia2.getFileName());
        assertTrue((mediaList.get(2).getFileSize() * 1024L) == testMedia2.getFileSize());

        verifyZeroInteractions(mediaSproc);
    }

    @Test
    public void testFilterDerivatives() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, true, true, true, true, true, null);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder().mediaGuid(guid1).lcmMediaId("1").domain("Lodging").domainId("1234").lastUpdated(new Date())
                .domainFields("{\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}").build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").domainId("1234")
                .lastUpdated(LocalDateTime.now().minusMinutes(5).toDate()).build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        MediaByDomainIdResponse testMediaList1 = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, "a,t,b", null, null, null);

        assertEquals(3, testMediaList1.getImages().size());
        testMediaList1.getImages().stream().filter(media -> media.getDomainFields() != null && media.getDomainFields().get("lcmMediaId") != null).forEach(media -> {
            assertEquals(1, media.getDerivatives().size());
            assertTrue(media.getDerivatives().get(0).get("type").equals("t"));
        });
        verifyZeroInteractions(mediaSproc);
    }

    @Test
    public void testBooleanPropertyHero() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList =  new ArrayList<>();
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);
        
        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder().mediaGuid(guid1).fileName("file1.jpg").domain("Lodging").domainId("1234").lastUpdated(new Date())
                .domainFields("{\"subcategoryId\":\"4321\",\"propertyHero\": true}").build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).fileName("file3.jpg").domain("Lodging").domainId("1234")
                .lastUpdated(LocalDateTime.now().minusMinutes(5).toDate()).build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        MediaByDomainIdResponse testMediaList1 = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, null, null, null, null);

        assertEquals(2, testMediaList1.getImages().size());
    }

    @Test
    public void testGetImageByDomainIdSameFileName() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList =  new ArrayList<>();
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder().mediaGuid(guid1).fileName("file1.jpg").domain("Lodging").domainId("1234").lastUpdated(new Date())
                .domainFields("{\"subcategoryId\":\"4321\",\"propertyHero\": true}").build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).fileName("file1.jpg").domain("Lodging").domainId("1234")
                .lastUpdated(LocalDateTime.now().minusMinutes(5).toDate()).build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        MediaByDomainIdResponse testMediaList1 = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, null, null, null, null);

        assertEquals(2, testMediaList1.getImages().size());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetMediaByDomainIdFileProcessFalse() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, true, false, false, false, true, null);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder().mediaGuid(guid1).lcmMediaId("1").domain("Lodging").domainId("1234").lastUpdated(new Date())
                .domainFields("{\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}").build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").domainId("1234")
                .lastUpdated(LocalDateTime.now().minusMinutes(5).toDate()).build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        MediaByDomainIdResponse testMediaResponse = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, null, null, null, null);

        assertEquals(3, testMediaResponse.getImages().size());
        DomainIdMedia testMedia1 = testMediaResponse.getImages().get(0);
        assertEquals(mediaList.get(0).getDomainId().toString(), testMediaResponse.getDomainId());
        assertEquals(mediaList.get(0).getLastUpdatedBy(), testMedia1.getLastUpdatedBy());
        assertEquals(mediaList.get(0).getFileName(), testMedia1.getFileName());
        assertEquals(dynamoMedia1.getMediaGuid(), testMedia1.getMediaGuid());
        assertEquals(mediaList.get(0).getComment(), testMedia1.getComments().get(0).getNote());
        assertTrue((mediaList.get(0).getFileSize() * 1024L) == testMedia1.getFileSize());
        assertEquals(0, testMedia1.getDerivatives().size());
        assertEquals("true", testMedia1.getDomainFields().get("propertyHero"));
        assertEquals("4321", testMedia1.getDomainFields().get("subcategoryId"));
        DomainIdMedia testMedia2 = testMediaResponse.getImages().get(2);
        assertNull(testMedia2.getMediaGuid());
        assertEquals(1, testMedia2.getDerivatives().size());
        HashMap derivative = (HashMap) testMedia2.getDerivatives().get(0);
        assertEquals(derivative.get("fileSize"), 200 * 1024L);
        assertEquals(derivative.get("width"), 20);
        assertEquals(derivative.get("height"), 21);
        assertEquals(derivative.get("type"), "s");
        assertEquals(derivative.get("location"), "https://media.int.expedia.com/image2_s.jpg");
        DomainIdMedia testMedia3 = testMediaResponse.getImages().get(1);
        assertEquals(dynamoMedia3.getMediaGuid(), testMedia3.getMediaGuid());
        assertNull(testMedia3.getDerivatives());
    }

    @Test
    public void testMediaDeleteNotFoundGUID() throws NoSuchFieldException, IllegalAccessException {
        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        doNothing().when(mockMediaDBRepo).deleteMedia(anyObject());
        when(mockMediaDBRepo.getMedia(anyString())).thenReturn(null);
        MediaDao mediaDao = makeMockMediaDao(null, null, mockMediaDBRepo, null, null, makeMockProcessLogDao());
        mediaDao.deleteMediaByGUID("d2d4d480-9627-47f9-86c6-1874c18d34t6");
        verify(mockMediaDBRepo, times(0)).deleteMedia(anyObject());
    }

    @Test
    public void testMediaDeleteFoundGUID() throws NoSuchFieldException, IllegalAccessException {
        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        doNothing().when(mockMediaDBRepo).deleteMedia(anyObject());
        when(mockMediaDBRepo.getMedia(anyString())).thenReturn(new Media());
        MediaDao mediaDao = makeMockMediaDao(null, null, mockMediaDBRepo, null, null, makeMockProcessLogDao());
        mediaDao.deleteMediaByGUID("d2d4d480-9627-47f9-86c6-1874c18d34t6");
        verify(mockMediaDBRepo, times(1)).deleteMedia(anyObject());
    }

    @Test
    public void testMediaDeleteByGUIDWithLcmMediaId() throws NoSuchFieldException, IllegalAccessException {

        final String lcmMediaId = "6545";

        SQLMediaDeleteSproc mockSqlMediaDeleteSproc = mock(SQLMediaDeleteSproc.class);
        doNothing().when(mockSqlMediaDeleteSproc).deleteMedia(anyInt());

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        doNothing().when(mockMediaDBRepo).deleteMedia(anyObject());
        Media media = new Media();
        media.setLcmMediaId(lcmMediaId);
        when(mockMediaDBRepo.getMedia(anyString())).thenReturn(media);

        MediaDao mediaDao = makeMockMediaDao(null, null, mockMediaDBRepo, null, null, makeMockProcessLogDao());
        setFieldValue(mediaDao, "lcmMediaDeleteSproc", mockSqlMediaDeleteSproc);

        mediaDao.deleteMediaByGUID(lcmMediaId);
        verify(mockSqlMediaDeleteSproc, times(1)).deleteMedia(Integer.valueOf(lcmMediaId));
    }
    
    @Test
    public void testMediaGetNotFoundGUID() throws NoSuchFieldException, IllegalAccessException {
        MediaDao mediaDao = makeMockMediaDao(null, null, mock(DynamoMediaRepository.class), null, null, makeMockProcessLogDao());
        assertNull(mediaDao.getMediaByGUID("d2d4d480-9627-47f9-86c6-1874c18d34t6"));
    }

    @Test
    public void testMediaGetNotFoundLCMMediaId() throws NoSuchFieldException, IllegalAccessException {
        SQLMediaGetSproc mockMediaGetSproc = mock(SQLMediaGetSproc.class);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(SQLMediaGetSproc.MEDIA_SET, new ArrayList<LcmMedia>());
        when(mockMediaGetSproc.execute(any(Integer.class))).thenReturn(resultMap);
        MediaDao mediaDao = makeMockMediaDao(null, null, mock(DynamoMediaRepository.class), null, mockMediaGetSproc, makeMockProcessLogDao());
        assertNull(mediaDao.getMediaByGUID("4321"));
    }

    @Test
    public void testMediaGetGUIDNoLcm() throws NoSuchFieldException, IllegalAccessException {
        final Properties mockProperties = new Properties();
        mockProperties.put("1", "EPC Internal User");

        String guid = "d2d4d480-9627-47f9-86c6-1874c18d34t6";
        Media guidMedia = Media.builder().active("true").mediaGuid(guid).lastUpdated(new Date()).fileName("super_potato.jpg").build();
        DynamoMediaRepository mediaDynamo = mock(DynamoMediaRepository.class);
        when(mediaDynamo.getMedia(guid)).thenReturn(guidMedia);
        
        MediaDao mediaDao = makeMockMediaDao(null, null, mediaDynamo, mockProperties, null, makeMockProcessLogDao());
        MediaGetResponse resultMedia = mediaDao.getMediaByGUID(guid);
        MediaGetResponse guidMediaGet = transformSingleMediaForResponse(guidMedia);
        assertEquals(guidMediaGet.getMediaGuid(), resultMedia.getMediaGuid());
        assertEquals(guidMediaGet.getFileName(), resultMedia.getFileName());
        assertEquals(guidMediaGet.getSourceUrl(), resultMedia.getSourceUrl());
        assertEquals(guidMediaGet.getActive(), resultMedia.getActive());
        assertEquals(guidMediaGet.getStatus(), resultMedia.getStatus());
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
        
        MediaDao mediaDao = makeMockMediaDao(null, mockMediaItemSproc, mock(DynamoMediaRepository.class), mockProperties, mockMediaGetSproc, makeMockProcessLogDao());
        MediaGetResponse resultMedia = mediaDao.getMediaByGUID(lcmMediaId.toString());
        assertEquals(lcmMediaId.toString(), resultMedia.getDomainFields().get("lcmMediaId"));
    }

    @Test
    public void testMediaGetGuidAndLcm() throws NoSuchFieldException, IllegalAccessException {
        final Properties mockProperties = new Properties();
        mockProperties.put("1", "EPC Internal User");

        Integer lcmMediaId = 54321;
        Integer domainId = 12345;

        Map<String, Object> domainFields = Maps.newHashMap();
        domainFields.put("originalField", "Original Value");
        domainFields.put("rooms", "Try to override LCM rooms");
        domainFields.put("propertyHero", "Try to override propertyHero");
        domainFields.put("subcategoryId", "Try to override subcategory Id");
        
        String guid = "d2d4d480-9627-47f9-86c6-1874c18d34t6";
        Media guidMedia = Media.builder().active("true").mediaGuid(guid).domain(Domain.LODGING.getDomain()).domainId(domainId.toString())
                .lcmMediaId(lcmMediaId.toString()).fileName("super_potato.jpg")
                .domainData(domainFields).build();
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
        
        MediaDao mediaDao = makeMockMediaDao(null, mockMediaItemSproc, mediaDynamo, mockProperties, null, makeMockProcessLogDao());
        MediaGetResponse resultMedia = mediaDao.getMediaByGUID(guid);
        assertEquals(lcmMediaId.toString(), resultMedia.getDomainFields().get("lcmMediaId"));

        // copy domain fields from dynamo when they don't conflict with LCM domain fields
        assertEquals(resultMedia.getDomainFields().get("originalField"), "Original Value");
        assertTrue(resultMedia.getDomainFields().get("rooms") instanceof List);
        assertNull(resultMedia.getDomainFields().get("propertyHero"));
        assertEquals(resultMedia.getDomainFields().get("subcategoryId"), "45");
    }

    @Test
    public void testMediaGetByDomainIdPagination() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, false, true, true, true, true, null);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);
        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        when(mockMediaDBRepo.loadMedia(any(), any())).thenReturn(createMedia());
        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());

        MediaByDomainIdResponse testMediaResponse = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", "true", null, null, 20, 2);

        assertTrue(testMediaResponse.getTotalMediaCount() == 50);
        assertNotEquals(testMediaResponse.getImages().get(1).getMediaGuid(), "0aaaaaaa-bbbb-bbbb-bbbb-1234-wwwwwwwwwwww");
        IntStream.range(0, 20)
                .forEach(i -> assertEquals(testMediaResponse.getImages().get(i).getMediaGuid(), (i + 19) + "aaaaaa-bbbb-bbbb-bbbb-1234-wwwwwwwwwwww"));
    }

    @Test
    public void testMediaGetByDomainIdPaginationOutOfBounds() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, false, true, true, true, true, null);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);
        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        when(mockMediaDBRepo.loadMedia(any(), any())).thenReturn(createMedia());
        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        try {
            mediaDao.getMediaByDomainId(Domain.LODGING, "1234", "true", null, null, 20, 3);
        } catch (Exception ex) {
            assertEquals("pageIndex is out of bounds", ex.getMessage());
        }

    }

    @Test
    public void testMediaGetByDomainIdPaginationPageSizeExistsButPageIndexNull() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, false, true, true, true, true, null);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);
        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        when(mockMediaDBRepo.loadMedia(any(), any())).thenReturn(createMedia());
        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        try {
            mediaDao.getMediaByDomainId(Domain.LODGING, "1234", "true", null, null, 20, null);
        } catch (Exception ex) {
            assertEquals("pageSize and pageIndex are inclusive, either both fields can be null or not null", ex.getMessage());
        }
    }

    @Test
    public void testMediaGetByDomainIdPaginationPageIndexExistsButPageSizeNull() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, false, true, true, true, true, null);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);
        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        when(mockMediaDBRepo.loadMedia(any(), any())).thenReturn(createMedia());
        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        try {
            mediaDao.getMediaByDomainId(Domain.LODGING, "1234", "true", null, null, null, 20);
        } catch (Exception ex) {
            assertEquals("pageSize and pageIndex are inclusive, either both fields can be null or not null", ex.getMessage());
        }
    }

    @Test
    public void testMediaGetByDomainIdPaginationIsNegative() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, false, true, true, true, true, null);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);
        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        when(mockMediaDBRepo.loadMedia(any(), any())).thenReturn(createMedia());
        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        setFieldValue(mediaDao, "roomGetByCatalogItemIdSproc", roomGetByCatalogItemIdSproc);
        try {
            mediaDao.getMediaByDomainId(Domain.LODGING, "1234", "true", null, null, -20, -1);
        } catch (Exception ex) {
            assertEquals("pageSize and pageIndex can only be positive integer values", ex.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetMediaByDomainIdProvidedName() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, false, true, true, true, true, 2);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder()
                .mediaGuid(guid1).domain("Lodging").domainId("1234").fileUrl("s3://fileUrl/imageName.jpg").providedName("sick_name.jpg").fileName("not_a_sick_name.jpg")
                .domainFields("{\"lcmMediaId\":\"1\",\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}")
                .derivatives("[{\"type\":\"v\",\"width\":179,\"height\":240,\"fileSize\":10622,\"location\":\"s3://ewe-cs-media-test/test/derivative/lodging/1000000/10000/7200/7139/dfec2df8_v.jpg\"}]")
                .lastUpdated(new Date())
                .build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").domainId("1234").lastUpdated(LocalDateTime.now().minusMinutes(5).toDate()).build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        MediaByDomainIdResponse testMediaRespomse = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, null, null, null, null);

        assertEquals(3, testMediaRespomse.getImages().size());
        testMediaRespomse.getImages().stream().filter(media -> media.getDomainFields() != null && media.getDomainFields().get("lcmMediaId") != null).forEach(media -> assertEquals(2, media.getDerivatives().size()));
        DomainIdMedia testMedia1 = testMediaRespomse.getImages().get(0);
        Map<String, Object> domainMap = testMedia1.getDomainFields();
        List rooms = (ArrayList)domainMap.get("rooms");
        assertEquals(((HashMap)rooms.get(0)).get("roomId"),"123");
        assertTrue(Boolean.valueOf(((HashMap) rooms.get(0)).get("roomHero").toString()));

        assertEquals(mediaList.get(0).getDomainId().toString(), testMediaRespomse.getDomainId());
        assertEquals(mediaList.get(0).getLastUpdatedBy(), testMedia1.getLastUpdatedBy());
        assertEquals("sick_name.jpg", testMedia1.getFileName());
        assertEquals(dynamoMedia1.getMediaGuid(), testMedia1.getMediaGuid());
        assertEquals(mediaList.get(0).getComment(), testMedia1.getComments().get(0).getNote());
        assertTrue((mediaList.get(0).getFileSize() * 1024L) == testMedia1.getFileSize());
        assertEquals("true", testMedia1.getDomainFields().get("propertyHero"));
        assertEquals("4321", testMedia1.getDomainFields().get("subcategoryId"));
        assertEquals("VirtualTour", testMedia1.getDomainDerivativeCategory());
        assertEquals("s3://fileUrl/imageName.jpg", testMedia1.getFileUrl());
        DomainIdMedia testMedia2 = testMediaRespomse.getImages().get(2);
        assertNull(testMedia2.getMediaGuid());
        assertNull(testMedia2.getFileUrl());
        DomainIdMedia testMedia3 = testMediaRespomse.getImages().get(1);
        assertEquals(dynamoMedia3.getMediaGuid(), testMedia3.getMediaGuid());
        assertNull(testMedia3.getFileUrl());
    }

    @Test
    public void testMediaGetGUIDProvidedName() throws NoSuchFieldException, IllegalAccessException {
        final Properties mockProperties = new Properties();
        mockProperties.put("1", "EPC Internal User");

        String guid = "d2d4d480-9627-47f9-86c6-1874c18d34t6";
        Media guidMedia = Media.builder().active("true").mediaGuid(guid).lastUpdated(new Date()).providedName("super_duper_potato.jpg").fileName("super_potato.jpg").build();
        DynamoMediaRepository mediaDynamo = mock(DynamoMediaRepository.class);
        when(mediaDynamo.getMedia(guid)).thenReturn(guidMedia);

        MediaDao mediaDao = makeMockMediaDao(null, null, mediaDynamo, mockProperties, null, makeMockProcessLogDao());
        MediaGetResponse resultMedia = mediaDao.getMediaByGUID(guid);
        MediaGetResponse guidMediaGet = transformSingleMediaForResponse(guidMedia);
        assertEquals(guidMediaGet.getMediaGuid(), resultMedia.getMediaGuid());
        assertEquals(guidMediaGet.getFileName(), resultMedia.getFileName());
        assertEquals(guidMediaGet.getSourceUrl(), resultMedia.getSourceUrl());
        assertEquals(guidMediaGet.getActive(), resultMedia.getActive());
        assertEquals(guidMediaGet.getStatus(), resultMedia.getStatus());
    }

    @Test
    public void testMediaPreviouslyPublished() throws Exception {
        final Properties mockProperties = new Properties();
        mockProperties.put("1", "EPC Internal User");

        String guid = "d2d4d480-9627-47f9-86c6-1874c18d34t6";
        Media guidMedia = Media.builder().active("true").mediaGuid(guid).lastUpdated(new Date()).providedName("super_duper_potato.jpg").fileName("super_potato.jpg").build();
        DynamoMediaRepository mediaDynamo = mock(DynamoMediaRepository.class);
        when(mediaDynamo.getMedia(guid)).thenReturn(guidMedia);

        MediaDao mediaDao = makeMockMediaDao(null, null, mediaDynamo, mockProperties, null, makeMockProcessLogPreviouslyPublishedDao());
        MediaGetResponse resultMedia = mediaDao.getMediaByGUID(guid);
        MediaGetResponse guidMediaGet = transformSingleMediaForResponse(guidMedia);
        assertEquals(guidMediaGet.getMediaGuid(), resultMedia.getMediaGuid());
        assertEquals(guidMediaGet.getFileName(), resultMedia.getFileName());
        assertEquals(guidMediaGet.getSourceUrl(), resultMedia.getSourceUrl());
        assertEquals(guidMediaGet.getActive(), resultMedia.getActive());
        assertEquals("PUBLISHED", resultMedia.getStatus());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetMediaByDomainIdFilterVTImages() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, false, true, true, true, true, 2);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder()
                .mediaGuid(guid1).domain("Lodging").domainId("1234").fileUrl("s3://fileUrl/imageName.jpg")
                .domainFields("{\"lcmMediaId\":\"1\",\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}")
                .derivatives("[{\"type\":\"v\",\"width\":179,\"height\":240,\"fileSize\":10622,\"location\":\"s3://ewe-cs-media-test/test/derivative/lodging/1000000/10000/7200/7139/dfec2df8_v.jpg\"}]")
                .lastUpdated(new Date())
                .build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").domainId("1234").lastUpdated(LocalDateTime.now().minusMinutes(5).toDate()).build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        MediaByDomainIdResponse testMediaRespomse = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, null, "Default", null, null);

        assertEquals(2, testMediaRespomse.getImages().size());
        testMediaRespomse.getImages().stream().filter(
                media -> media.getDomainFields() != null && media.getDomainFields().get("lcmMediaId") != null).forEach(
                media -> assertEquals(2, media.getDerivatives().size()));
        assertFalse(testMediaRespomse.getImages().stream().filter(media -> ("VirtualTour").equals(media.getDomainDerivativeCategory())).findAny().isPresent());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testGetMediaByDomainIdOnlyVTImages() throws Exception {
        SQLMediaListSproc mediaListSproc = mock(SQLMediaListSproc.class);
        List<LcmMediaAndDerivative> mediaList = make2Media2DerivativeMediaResult(true, false, true, true, true, true, 2);
        Map<String, Object> mediaResult = new HashMap<>();
        mediaResult.put(SQLMediaListSproc.MEDIA_SET, mediaList);
        when(mediaListSproc.execute(anyInt())).thenReturn(mediaResult);

        SQLMediaItemGetSproc mediaSproc = mock(SQLMediaItemGetSproc.class);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        Media dynamoMedia1 = Media.builder()
                .mediaGuid(guid1).domain("Lodging").domainId("1234").fileUrl("s3://fileUrl/imageName.jpg")
                .domainFields("{\"lcmMediaId\":\"1\",\"subcategoryId\":\"4321\",\"propertyHero\": \"true\"}")
                .derivatives("[{\"type\":\"v\",\"width\":179,\"height\":240,\"fileSize\":10622,\"location\":\"s3://ewe-cs-media-test/test/derivative/lodging/1000000/10000/7200/7139/dfec2df8_v.jpg\"}]")
                .lastUpdated(new Date())
                .build();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        Media dynamoMedia3 = Media.builder().mediaGuid(guid3).domain("Lodging").domainId("1234").lastUpdated(LocalDateTime.now().minusMinutes(5).toDate()).build();
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(any(), anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        MediaDao mediaDao = makeMockMediaDao(mediaListSproc, mediaSproc, mockMediaDBRepo, properties, null, makeMockProcessLogDao());
        MediaByDomainIdResponse testMediaRespomse = mediaDao.getMediaByDomainId(Domain.LODGING, "1234", null, null, "VirtualTours", null, null);

        assertEquals(1, testMediaRespomse.getImages().size());
        testMediaRespomse.getImages().stream().filter(
                media -> media.getDomainFields() != null && media.getDomainFields().get("lcmMediaId") != null).forEach(
                media -> assertEquals(2, media.getDerivatives().size()));
        assertFalse(testMediaRespomse.getImages().stream().filter(media -> !("VirtualTour").equals(media.getDomainDerivativeCategory())).findAny().isPresent());
    }

    @Test
    public void testValidatePaginationPageIndexOutOfBounds() throws InvocationTargetException, IllegalAccessException {

        LcmDynamoMediaDao lcmDynamoMediaDao = new LcmDynamoMediaDao();
        Method method = ReflectionUtils.findMethod(LcmDynamoMediaDao.class, "validatePagination", Integer.class, Integer.class, Integer.class);
        method.setAccessible(true);
        String validation = (String) method.invoke(lcmDynamoMediaDao, 20, 3, 21);
        assertEquals("pageIndex is out of bounds", validation);
    }

    @Test
    public void testValidatePaginationPageSizeGreaterThanTotalMediaCount() throws InvocationTargetException, IllegalAccessException {

        LcmDynamoMediaDao lcmDynamoMediaDao = new LcmDynamoMediaDao();
        Method method = ReflectionUtils.findMethod(LcmDynamoMediaDao.class, "validatePagination", Integer.class, Integer.class, Integer.class);
        method.setAccessible(true);
        String validation = (String) method.invoke(lcmDynamoMediaDao, 20, 6, 4);
        assertNull(validation);
    }

    @Test
    public void testPaginateItemsPageSizeGreaterThanRemainingMedia() throws InvocationTargetException, IllegalAccessException {

        LcmDynamoMediaDao lcmDynamoMediaDao = new LcmDynamoMediaDao();
        Method method = ReflectionUtils.findMethod(LcmDynamoMediaDao.class, "paginateItems", Stream.class, Integer.class, Integer.class);
        method.setAccessible(true);
        Stream items = Arrays.stream(new Integer[12]);
        Stream stream = (Stream) method.invoke(lcmDynamoMediaDao, items, 5, 3);
        assertEquals(2, stream.count());
    }

    @Test
    public void testPaginateItemsPageSizeGreaterThanTotalMedia() throws InvocationTargetException, IllegalAccessException {

        LcmDynamoMediaDao lcmDynamoMediaDao = new LcmDynamoMediaDao();
        Method method = ReflectionUtils.findMethod(LcmDynamoMediaDao.class, "paginateItems", Stream.class, Integer.class, Integer.class);
        method.setAccessible(true);
        Stream items = Arrays.stream(new Integer[12]);
        Stream stream = (Stream) method.invoke(lcmDynamoMediaDao, items, 13, 1);
        assertEquals(12, stream.count());
    }

    @Test
    public void testPaginateItemsPageSizeLessThanTotalMedia() throws InvocationTargetException, IllegalAccessException {

        LcmDynamoMediaDao lcmDynamoMediaDao = new LcmDynamoMediaDao();
        Method method = ReflectionUtils.findMethod(LcmDynamoMediaDao.class, "paginateItems", Stream.class, Integer.class, Integer.class);
        method.setAccessible(true);
        Stream items = Arrays.stream(new Integer[12]);
        Stream stream = (Stream) method.invoke(lcmDynamoMediaDao, items, 5, 1);
        assertEquals(5, stream.count());
    }

    @Test
    public void testPaginateItemsPageSizeLessThanRemainingMedia() throws InvocationTargetException, IllegalAccessException {

        LcmDynamoMediaDao lcmDynamoMediaDao = new LcmDynamoMediaDao();
        Method method = ReflectionUtils.findMethod(LcmDynamoMediaDao.class, "paginateItems", Stream.class, Integer.class, Integer.class);
        method.setAccessible(true);
        Stream items = Arrays.stream(new Integer[12]);
        Stream stream = (Stream) method.invoke(lcmDynamoMediaDao, items, 5, 2);
        assertEquals(5, stream.count());
    }
    
    private MediaDao makeMockMediaDao(SQLMediaListSproc mediaIdSproc, SQLMediaItemGetSproc mediaItemSproc, DynamoMediaRepository mockMediaDBRepo,
                                      final Properties properties, SQLMediaGetSproc mediaGetSproc, LcmProcessLogDao processLogDao) throws NoSuchFieldException, IllegalAccessException {
        MediaDao mediaDao = new LcmDynamoMediaDao();
        
        setFieldValue(mediaDao, "lcmMediaListSproc", mediaIdSproc);
        setFieldValue(mediaDao, "lcmMediaSproc", mediaGetSproc);
        setFieldValue(mediaDao, "lcmMediaItemSproc", mediaItemSproc);
        setFieldValue(mediaDao, "paramLimit", 50);

        setFieldValue(mediaDao, "mediaRepo", mockMediaDBRepo);
        setFieldValue(mediaDao, "providerProperties", properties);
        setFieldValue(mediaDao, "processLogDao", processLogDao);
        setFieldValue(mediaDao, "activityWhiteList", makeActivityWhitelist());
        setFieldValue(mediaDao, "imageRootPath", "https://media.int.expedia.com/");
        setFieldValue(mediaDao, "roomGetByMediaIdSproc", roomGetByMediaIdSproc);
        setFieldValue(mediaDao, "roomGetByCatalogItemIdSproc", roomGetByCatalogItemIdSproc);
        return mediaDao;
    }

    private LcmProcessLogDao makeMockProcessLogPreviouslyPublishedDao() {
        LcmProcessLogDao mockProcessLogDao = mock(LcmProcessLogDao.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus1 = new MediaProcessLog("2014-07-29 10:08:11.6890000 -07:00", "image1.jpg", "Something", "Lodging");
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        MediaProcessLog mediaLogStatus2 = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "image1.jpg", "Publish", "Lodging");
        mediaLogStatuses.add(mediaLogStatus2);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus1);
        MediaProcessLog mediaLogStatus3 = new MediaProcessLog("2014-07-29 10:08:14.6890000 -07:00", "image1.jpg", "Reject", "Lodging");
        mediaLogStatuses.add(mediaLogStatus3);
        when(mockProcessLogDao.findMediaStatus(any())).thenReturn(mediaLogStatuses);
        return mockProcessLogDao;
    }

    private List<ActivityMapping> makeActivityWhitelist() {
        List<ActivityMapping> whitelist = new ArrayList<>();
        ActivityMapping activityMapping1 = new ActivityMapping();
        activityMapping1.setActivityType("Publish");
        activityMapping1.setMediaType(".*");
        activityMapping1.setStatusMessage("PUBLISHED");
        ActivityMapping activityMapping2 = new ActivityMapping();
        activityMapping2.setActivityType("Reject");
        activityMapping2.setMediaType(".*");
        activityMapping2.setStatusMessage("REJECTED");
        whitelist.add(activityMapping1);
        whitelist.add(activityMapping2);
        return whitelist;
    }

    private LcmProcessLogDao makeMockProcessLogDao() {
        LcmProcessLogDao mockProcessLogDao = mock(LcmProcessLogDao.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus1 = new MediaProcessLog("2014-07-29 10:08:11.6890000 -07:00", "image1.jpg", "Something", "Lodging");
        mediaLogStatuses.add(mediaLogStatus1);
        MediaProcessLog mediaLogStatus2 = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "image1.jpg", "Publish", "Lodging");
        mediaLogStatuses.add(mediaLogStatus2);
        when(mockProcessLogDao.findMediaStatus(any())).thenReturn(mediaLogStatuses);
        return mockProcessLogDao;
    }

    private MediaGetResponse transformSingleMediaForResponse(Media media) {
        /* @formatter:off */
        setResponseLcmMediaId(media);
        return MediaGetResponse.builder()
                .mediaGuid(media.getMediaGuid())
                .fileUrl(media.getFileUrl())
                .sourceUrl(media.getSourceUrl())
                .fileName(FileNameUtil.resolveFileNameToDisplay(media))
                .active(media.getActive())
                .width(media.getWidth())
                .height(media.getHeight())
                .fileSize(media.getFileSize())
                .status(media.getStatus())
                .lastUpdatedBy(media.getUserId())
                .lastUpdateDateTime(DATE_FORMATTER.print(media.getLastUpdated().getTime()))
                .domain(media.getDomain())
                .domainId(media.getDomainId())
                .domainProvider(media.getProvider())
                .domainFields(media.getDomainData())
                .derivatives(media.getDerivativesList())
                .domainDerivativeCategory(media.getDomainDerivativeCategory())
                .comments((media.getCommentList() == null) ? null : media.getCommentList().stream()
                        .map(comment -> Comment.builder().note(comment)
                                .timestamp(DATE_FORMATTER.print(media.getLastUpdated().getTime())).build())
                        .collect(Collectors.toList()))
                .build();
        /* @formatter:on */
    }

    private void setResponseLcmMediaId(Media media) {
        if (media.getLcmMediaId() != null) {
            if (media.getDomainData() == null) {
                media.setDomainData(new HashMap<>());
            }
            media.getDomainData().put(RESPONSE_FIELD_LCM_MEDIA_ID, media.getLcmMediaId());
        }
    }

    private List<Media> createMedia() {
        List<String> commentList = new LinkedList<>();
        commentList.add("Comment1");
        commentList.add("Comment2");
        List<Media> mediaValues = new ArrayList<>();
        IntStream.range(0, 49)
                .forEach( i -> {
                            Media mediaItem = Media.builder().active("true").domain("Lodging").domainId("1234").fileName("1234_file" + i + "_name.jpg")
                                    .mediaGuid(i + ((i > 9) ? "aaaaaa" : "aaaaaaa") + "-bbbb-bbbb-bbbb-1234-wwwwwwwwwwww").lastUpdated(new Date()).commentList(commentList).build();
                            mediaValues.add(mediaItem);
                        }
                );
        return mediaValues;
    }

    private List<LcmMediaAndDerivative> make2Media2DerivativeMediaResult(boolean active1, boolean active2, boolean published11, boolean published12, boolean published21, boolean published22, Integer formatId) {
        LcmMediaAndDerivative media1d1 = LcmMediaAndDerivative.builder().mediaId(1).domainId(1234).fileName("image1.jpg").active(active1).width(20)
                .height(21).fileSize(200).lastUpdatedBy("test").lastUpdateDate(new Date()).mediaLastUpdatedBy("test").mediaLastUpdateDate(new Date())
                .provider(400).category(3).comment("Comment").fileProcessed(published11).derivativeSizeTypeId(1).derivativeFileName("image1_t.jpg")
                .derivativeWidth(10).derivativeHeight(11).derivativeFileSize(100).formatId(formatId).build();
        LcmMediaAndDerivative media1d2 = LcmMediaAndDerivative.builder().mediaId(1).domainId(1234).fileName("image1.jpg").active(active1).width(20)
                .height(21).fileSize(200).lastUpdatedBy("test").lastUpdateDate(new Date()).mediaLastUpdatedBy("test").mediaLastUpdateDate(new Date())
                .provider(400).category(3).comment("Comment").fileProcessed(published12).derivativeSizeTypeId(2).derivativeFileName("image1_s.jpg")
                .derivativeWidth(20).derivativeHeight(21).derivativeFileSize(200).formatId(formatId).build();
        LcmMediaAndDerivative media2d1 = LcmMediaAndDerivative.builder().mediaId(2).domainId(1234).fileName("image2.jpg").active(active2).width(40)
                .height(41).fileSize(500).lastUpdatedBy("test").lastUpdateDate(new Date()).mediaLastUpdatedBy("test").mediaLastUpdateDate(new Date())
                .provider(400).category(500).comment("Comment").fileProcessed(published21).derivativeSizeTypeId(1).derivativeFileName("image2_t.jpg")
                .derivativeWidth(10).derivativeHeight(11).derivativeFileSize(100).build();
        LcmMediaAndDerivative media2d2 = LcmMediaAndDerivative.builder().mediaId(2).domainId(1234).fileName("image2.jpg").active(active2).width(40)
                .height(41).fileSize(500).lastUpdatedBy("test").lastUpdateDate(new Date()).mediaLastUpdatedBy("test").mediaLastUpdateDate(new Date())
                .provider(400).category(500).comment("Comment").fileProcessed(published22).derivativeSizeTypeId(2).derivativeFileName("image2_s.jpg")
                .derivativeWidth(20).derivativeHeight(21).derivativeFileSize(200).build();
        List<LcmMediaAndDerivative> mediaList = new ArrayList<>();
        mediaList.add(media1d1);
        mediaList.add(media1d2);
        mediaList.add(media2d1);
        mediaList.add(media2d2);
        return mediaList;
    }

}
