package com.expedia.content.media.processing.services.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaDerivative;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaIdListSproc;

public class MediaDaoTest {

    @Test
    public void test() {
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
        LcmMedia media1 = new LcmMedia(1, 1, "image1.jpg", true, 20, 21, 200, "test", new Date(), 400, 500, null);
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
        LcmMedia media2 = new LcmMedia(2, 2, "image2.jpg", false, 40, 41, 400, "test", new Date(), 400, 500, null);
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

        MediaDao mediaDao = new MediaDao(mediaIdSproc, mediaSproc);
        List<LcmMedia> testMediaList = mediaDao.getMediaByDomainId("Lodging", "1234", null, null);

        assertEquals(2, testMediaList.size());
        testMediaList.stream().forEach(media -> assertEquals(2, media.getDerivatives().size()));
        LcmMedia testMedia1 = testMediaList.get(0);
        assertEquals(media1.getDomainId(), testMedia1.getDomainId());
        assertEquals(media1.getLastUpdatedBy(), testMedia1.getLastUpdatedBy());
        assertEquals(media1.getFileName(), testMedia1.getFileName());
        assertEquals(media1.getFileSize(), testMedia1.getFileSize());
    }

    @Test
    public void testFilterActive() {
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
        LcmMedia media1 = new LcmMedia(1, 1, "image1.jpg", true, 20, 21, 200, "test", new Date(), 400, 500, null);
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
        LcmMedia media2 = new LcmMedia(2, 2, "image2.jpg", false, 40, 41, 400, "test", new Date(), 400, 500, null);
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

        MediaDao mediaDao = new MediaDao(mediaIdSproc, mediaSproc);
        List<LcmMedia> testMediaList1 = mediaDao.getMediaByDomainId("Lodging", "1234", "true", null);

        assertEquals(1, testMediaList1.size());
        testMediaList1.stream().forEach(media -> assertEquals(2, media.getDerivatives().size()));
        LcmMedia testMedia1 = testMediaList1.get(0);
        assertEquals(media1.getDomainId(), testMedia1.getDomainId());
        assertEquals(media1.getLastUpdatedBy(), testMedia1.getLastUpdatedBy());
        assertEquals(media1.getFileName(), testMedia1.getFileName());
        assertEquals(media1.getFileSize(), testMedia1.getFileSize());

        List<LcmMedia> testMediaList2 = mediaDao.getMediaByDomainId("Lodging", "1234", "false", null);

        assertEquals(1, testMediaList2.size());
        testMediaList2.stream().forEach(media -> assertEquals(2, media.getDerivatives().size()));
        LcmMedia testMedia2 = testMediaList2.get(0);
        assertEquals(media2.getDomainId(), testMedia2.getDomainId());
        assertEquals(media2.getLastUpdatedBy(), testMedia2.getLastUpdatedBy());
        assertEquals(media2.getFileName(), testMedia2.getFileName());
        assertEquals(media2.getFileSize(), testMedia2.getFileSize());

    }

    @Test
    public void testFilterDerivatives() {
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
        LcmMedia media1 = new LcmMedia(1, 1, "image1.jpg", true, 20, 21, 200, "test", new Date(), 400, 500, null);
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
        LcmMedia media2 = new LcmMedia(2, 2, "image2.jpg", false, 40, 41, 400, "test", new Date(), 400, 500, null);
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

        MediaDao mediaDao = new MediaDao(mediaIdSproc, mediaSproc);
        List<LcmMedia> testMediaList1 = mediaDao.getMediaByDomainId("Lodging", "1234", null, "a,t,b");

        assertEquals(2, testMediaList1.size());
        testMediaList1.stream().forEach(media -> {
            assertEquals(1, media.getDerivatives().size());
            assertTrue(media.getDerivatives().get(0).getMediSizeType().equals("t"));
        });
    }

}
