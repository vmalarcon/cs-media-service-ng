package com.expedia.content.media.processing.services.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaDerivative;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaIdListSproc;
import com.expedia.content.media.processing.services.util.ActivityMapping;

public class MediaDaoTest {

    @Test
    public void test() throws Exception {
        SQLMediaIdListSproc mediaIdSproc = mock(SQLMediaIdListSproc.class);
        Integer mediaId1 = 1;
        Integer mediaId2 = 2;
        List<Integer> mediaIds = new ArrayList<>();
        mediaIds.add(mediaId1);
        mediaIds.add(mediaId2);
        Map<String, Object> idResult = new HashMap<>();
        idResult.put(SQLMediaIdListSproc.MEDIA_ID_SET, mediaIds);
        when(mediaIdSproc.execute(anyInt(), anyString())).thenReturn(idResult);

        SQLMediaGetSproc mediaSproc = mock(SQLMediaGetSproc.class);
        Map<String, Object> mediaResult1 = new HashMap<>();
        List<LcmMedia> mediaList1 = new ArrayList<>();
        LcmMedia media1 = new LcmMedia(1234, 1, "image1.jpg", true, 20, 21, 200, "test", new Date(), 400, 3, "Comment");
        mediaList1.add(media1);
        mediaResult1.put(SQLMediaGetSproc.MEDIA_SET, mediaList1);
        List<LcmMediaDerivative> derivativeList1 = new ArrayList<>();
        LcmMediaDerivative derivative11 = new LcmMediaDerivative(1, 1, true, "image1_t.jpg", 10, 11, 100);
        derivativeList1.add(derivative11);
        LcmMediaDerivative derivative12 = new LcmMediaDerivative(1, 2, true, "image1_s.jpg", 30, 31, 300);
        derivativeList1.add(derivative12);
        mediaResult1.put(SQLMediaGetSproc.MEDIA_DERIVATIVES_SET, derivativeList1);

        Map<String, Object> mediaResult2 = new HashMap<>();
        List<LcmMedia> mediaList2 = new ArrayList<>();
        LcmMedia media2 = new LcmMedia(1234, 2, "image2.jpg", false, 40, 41, 400, "test", new Date(), 400, 500, "");
        mediaList2.add(media2);
        mediaResult2.put(SQLMediaGetSproc.MEDIA_SET, mediaList2);
        List<LcmMediaDerivative> derivativeList2 = new ArrayList<>();
        LcmMediaDerivative derivative21 = new LcmMediaDerivative(2, 1, true, "image2_t.jpg", 50, 51, 500);
        derivativeList2.add(derivative21);
        LcmMediaDerivative derivative22 = new LcmMediaDerivative(2, 2, true, "image2_s.jpg", 60, 61, 600);
        derivativeList2.add(derivative22);
        mediaResult2.put(SQLMediaGetSproc.MEDIA_DERIVATIVES_SET, derivativeList2);

        when(mediaSproc.execute(anyInt(), eq(mediaId1))).thenReturn(mediaResult1);
        when(mediaSproc.execute(anyInt(), eq(mediaId2))).thenReturn(mediaResult2);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        Media dynamoMedia1 = new Media();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        dynamoMedia1.setMediaGuid(guid1);
        dynamoMedia1.setLcmMediaId("1");
        dynamoMedia1.setDomain("Lodging");
        dynamoMedia1.setDomainId("1234");
        dynamoMedia1.setDomainFields("{\"categoryId\":\"4321\",\"propertyHero\": \"true\"}");
        Media dynamoMedia3 = new Media();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        dynamoMedia3.setMediaGuid(guid3);
        dynamoMedia3.setDomain("Lodging");
        dynamoMedia3.setDomainId("1234");
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        LcmProcessLogDao mockProcessLogDao = mock(LcmProcessLogDao.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<MediaProcessLog>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "Publish", "1037678_109010ice.jpg", "Lodging");
        mediaLogStatuses.add(mediaLogStatus);
        when(mockProcessLogDao.findMediaStatus(any())).thenReturn(mediaLogStatuses);

        List<ActivityMapping> whitelist = new ArrayList<>();
        ActivityMapping activityMapping = new ActivityMapping();
        activityMapping.setActivityType("Publish");
        activityMapping.setMediaType(".*");
        activityMapping.setStatusMessage("PUBLISHED");
        whitelist.add(activityMapping);

        MediaDao mediaDao = new MediaDao();
        setFieldValue(mediaDao, "lcmMediaIdSproc", mediaIdSproc);
        setFieldValue(mediaDao, "lcmMediaSproc", mediaSproc);
        setFieldValue(mediaDao, "mediaRepo", mockMediaDBRepo);
        setFieldValue(mediaDao, "providerProperties", properties);
//        setFieldValue(mediaDao, "processLogDao", mockProcessLogDao);
//        setFieldValue(mediaDao, "activityWhiteList", whitelist);
        List<Media> testMediaList = mediaDao.getMediaByDomainId("Lodging", "1234", null, null);

        assertEquals(3, testMediaList.size());
        testMediaList.stream().filter(media -> media.getLcmMediaId() != null).forEach(media -> assertEquals(2, media.getDerivativesList().size()));
        Media testMedia1 = testMediaList.get(0);
        assertEquals(media1.getDomainId().toString(), testMedia1.getDomainId());
        assertEquals(media1.getLastUpdatedBy(), testMedia1.getUserId());
        assertEquals(media1.getFileName(), testMedia1.getFileName());
        assertEquals(dynamoMedia1.getMediaGuid(), testMedia1.getMediaGuid());
        assertEquals(media1.getComment(), testMedia1.getCommentList().get(0));
        assertTrue((media1.getFileSize() * 1024L) == testMedia1.getFileSize());
        assertEquals("true", testMedia1.getDomainData().get("propertyHero"));
        assertEquals("4321", testMedia1.getDomainData().get("categoryId"));
        Media testMedia2 = testMediaList.get(1);
        assertNull(testMedia2.getMediaGuid());
        assertNull(testMedia2.getCommentList());
        Media testMedia3 = testMediaList.get(2);
        assertEquals(dynamoMedia3.getMediaGuid(), testMedia3.getMediaGuid());
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

        SQLMediaGetSproc mediaSproc = mock(SQLMediaGetSproc.class);
        Map<String, Object> mediaResult1 = new HashMap<>();
        List<LcmMedia> mediaList1 = new ArrayList<>();
        LcmMedia media1 = new LcmMedia(1234, 1, "image1.jpg", true, 20, 21, 200, "test", new Date(), 400, 500, "Comment");
        mediaList1.add(media1);
        mediaResult1.put(SQLMediaGetSproc.MEDIA_SET, mediaList1);
        List<LcmMediaDerivative> derivativeList1 = new ArrayList<>();
        LcmMediaDerivative derivative11 = new LcmMediaDerivative(1, 1, true, "image1_t.jpg", 10, 11, 100);
        derivativeList1.add(derivative11);
        LcmMediaDerivative derivative12 = new LcmMediaDerivative(1, 2, true, "image1_s.jpg", 30, 31, 300);
        derivativeList1.add(derivative12);
        mediaResult1.put(SQLMediaGetSproc.MEDIA_DERIVATIVES_SET, derivativeList1);

        Map<String, Object> mediaResult2 = new HashMap<>();
        List<LcmMedia> mediaList2 = new ArrayList<>();
        LcmMedia media2 = new LcmMedia(1234, 2, "image2.jpg", false, 40, 41, 400, "test", new Date(), 400, 500, null);
        mediaList2.add(media2);
        mediaResult2.put(SQLMediaGetSproc.MEDIA_SET, mediaList2);
        List<LcmMediaDerivative> derivativeList2 = new ArrayList<>();
        LcmMediaDerivative derivative21 = new LcmMediaDerivative(2, 1, true, "image2_t.jpg", 50, 51, 500);
        derivativeList2.add(derivative21);
        LcmMediaDerivative derivative22 = new LcmMediaDerivative(2, 2, true, "image2_s.jpg", 60, 61, 600);
        derivativeList2.add(derivative22);
        mediaResult2.put(SQLMediaGetSproc.MEDIA_DERIVATIVES_SET, derivativeList2);

        when(mediaSproc.execute(anyInt(), eq(mediaId1))).thenReturn(mediaResult1);
        when(mediaSproc.execute(anyInt(), eq(mediaId2))).thenReturn(mediaResult2);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        Media dynamoMedia1 = new Media();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        dynamoMedia1.setMediaGuid(guid1);
        dynamoMedia1.setLcmMediaId("1");
        dynamoMedia1.setDomain("Lodging");
        dynamoMedia1.setDomainId("1234");
        Media dynamoMedia3 = new Media();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        dynamoMedia3.setMediaGuid(guid3);
        dynamoMedia3.setDomain("Lodging");
        dynamoMedia3.setDomainId("1234");
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        LcmProcessLogDao mockProcessLogDao = mock(LcmProcessLogDao.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<MediaProcessLog>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "Publish", "1037678_109010ice.jpg", "Lodging");
        mediaLogStatuses.add(mediaLogStatus);
        when(mockProcessLogDao.findMediaStatus(any())).thenReturn(mediaLogStatuses);

        List<ActivityMapping> whitelist = new ArrayList<>();
        ActivityMapping activityMapping = new ActivityMapping();
        activityMapping.setActivityType("Publish");
        activityMapping.setMediaType(".*");
        activityMapping.setStatusMessage("PUBLISHED");
        whitelist.add(activityMapping);

        MediaDao mediaDao = new MediaDao();
        setFieldValue(mediaDao, "lcmMediaIdSproc", mediaIdSproc);
        setFieldValue(mediaDao, "lcmMediaSproc", mediaSproc);
        setFieldValue(mediaDao, "mediaRepo", mockMediaDBRepo);
        setFieldValue(mediaDao, "providerProperties", properties);
//        setFieldValue(mediaDao, "processLogDao", mockProcessLogDao);
//        setFieldValue(mediaDao, "activityWhiteList", whitelist);
        setFieldValue(mediaDao, "lcmMediaIdSproc", mediaIdSproc);
        setFieldValue(mediaDao, "lcmMediaSproc", mediaSproc);
        List<Media> testMediaList1 = mediaDao.getMediaByDomainId("Lodging", "1234", "true", null);

        assertEquals(1, testMediaList1.size());
        testMediaList1.stream().forEach(media -> assertEquals(2, media.getDerivativesList().size()));
        Media testMedia1 = testMediaList1.get(0);
        assertEquals(media1.getDomainId().toString(), testMedia1.getDomainId());
        assertEquals(media1.getLastUpdatedBy(), testMedia1.getUserId());
        assertEquals(media1.getFileName(), testMedia1.getFileName());
        assertEquals(dynamoMedia1.getMediaGuid(), testMedia1.getMediaGuid());
        assertTrue((media1.getFileSize() * 1024L) == testMedia1.getFileSize());

        List<Media> testMediaList2 = mediaDao.getMediaByDomainId("Lodging", "1234", "false", null);

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

        SQLMediaGetSproc mediaSproc = mock(SQLMediaGetSproc.class);
        Map<String, Object> mediaResult1 = new HashMap<>();
        List<LcmMedia> mediaList1 = new ArrayList<>();
        LcmMedia media1 = new LcmMedia(1234, 1, "image1.jpg", true, 20, 21, 200, "test", new Date(), 400, 500, "Comment");
        mediaList1.add(media1);
        mediaResult1.put(SQLMediaGetSproc.MEDIA_SET, mediaList1);
        List<LcmMediaDerivative> derivativeList1 = new ArrayList<>();
        LcmMediaDerivative derivative11 = new LcmMediaDerivative(1, 1, true, "image1_t.jpg", 10, 11, 100);
        derivativeList1.add(derivative11);
        LcmMediaDerivative derivative12 = new LcmMediaDerivative(1, 2, true, "image1_s.jpg", 30, 31, 300);
        derivativeList1.add(derivative12);
        mediaResult1.put(SQLMediaGetSproc.MEDIA_DERIVATIVES_SET, derivativeList1);

        Map<String, Object> mediaResult2 = new HashMap<>();
        List<LcmMedia> mediaList2 = new ArrayList<>();
        LcmMedia media2 = new LcmMedia(1234, 2, "image2.jpg", false, 40, 41, 400, "test", new Date(), 400, 500, null);
        mediaList2.add(media2);
        mediaResult2.put(SQLMediaGetSproc.MEDIA_SET, mediaList2);
        List<LcmMediaDerivative> derivativeList2 = new ArrayList<>();
        LcmMediaDerivative derivative21 = new LcmMediaDerivative(2, 1, true, "image2_t.jpg", 50, 51, 500);
        derivativeList2.add(derivative21);
        LcmMediaDerivative derivative22 = new LcmMediaDerivative(2, 2, true, "image2_s.jpg", 60, 61, 600);
        derivativeList2.add(derivative22);
        mediaResult2.put(SQLMediaGetSproc.MEDIA_DERIVATIVES_SET, derivativeList2);

        when(mediaSproc.execute(anyInt(), eq(mediaId1))).thenReturn(mediaResult1);
        when(mediaSproc.execute(anyInt(), eq(mediaId2))).thenReturn(mediaResult2);

        DynamoMediaRepository mockMediaDBRepo = mock(DynamoMediaRepository.class);
        List<Media> dynamoMediaList = new ArrayList<>();
        Media dynamoMedia1 = new Media();
        String guid1 = "d2d4d480-9627-47f9-86c6-1874c18d3aaa";
        dynamoMedia1.setMediaGuid(guid1);
        dynamoMedia1.setLcmMediaId("1");
        dynamoMedia1.setDomain("Lodging");
        dynamoMedia1.setDomainId("1234");
        Media dynamoMedia3 = new Media();
        String guid3 = "d2d4d480-9627-47f9-86c6-1874c18d3bbb";
        dynamoMedia3.setMediaGuid(guid3);
        dynamoMedia3.setDomain("Lodging");
        dynamoMedia3.setDomainId("1234");
        dynamoMediaList.add(dynamoMedia1);
        dynamoMediaList.add(dynamoMedia3);
        when(mockMediaDBRepo.loadMedia(anyString())).thenReturn(dynamoMediaList);

        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");

        LcmProcessLogDao mockProcessLogDao = mock(LcmProcessLogDao.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<MediaProcessLog>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "Publish", "1037678_109010ice.jpg", "Lodging");
        mediaLogStatuses.add(mediaLogStatus);
        when(mockProcessLogDao.findMediaStatus(any())).thenReturn(mediaLogStatuses);

        List<ActivityMapping> whitelist = new ArrayList<>();
        ActivityMapping activityMapping = new ActivityMapping();
        activityMapping.setActivityType("Publish");
        activityMapping.setMediaType(".*");
        activityMapping.setStatusMessage("PUBLISHED");
        whitelist.add(activityMapping);

        MediaDao mediaDao = new MediaDao();
        setFieldValue(mediaDao, "lcmMediaIdSproc", mediaIdSproc);
        setFieldValue(mediaDao, "lcmMediaSproc", mediaSproc);
        setFieldValue(mediaDao, "mediaRepo", mockMediaDBRepo);
        setFieldValue(mediaDao, "providerProperties", properties);
//        setFieldValue(mediaDao, "processLogDao", mockProcessLogDao);
//        setFieldValue(mediaDao, "activityWhiteList", whitelist);
        setFieldValue(mediaDao, "lcmMediaIdSproc", mediaIdSproc);
        setFieldValue(mediaDao, "lcmMediaSproc", mediaSproc);
        List<Media> testMediaList1 = mediaDao.getMediaByDomainId("Lodging", "1234", null, "a,t,b");

        assertEquals(3, testMediaList1.size());
        testMediaList1.stream().filter(media -> media.getLcmMediaId() != null).forEach(media -> {
            assertEquals(1, media.getDerivativesList().size());
            assertTrue(media.getDerivativesList().get(0).get("type").equals("t"));
        });
    }

    private static void setFieldValue(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        final Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

}
