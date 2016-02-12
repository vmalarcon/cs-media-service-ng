package com.expedia.content.media.processing.services.dao;

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
        List<Media> mediaList1 = new ArrayList<>();
        Media media1 = new Media(1, 1, "image1.jpg", true, 20, 21, 200, "test", new Date(), 400, 500, null);
        mediaList1.add(media1);
        mediaResult1.put(SQLMediaGetSproc.MEDIA_SET, mediaList1);
        List<MediaDerivative> derivativeList1 = new ArrayList<>();
        MediaDerivative derivative11 = new MediaDerivative(1, 1, true, "image1_t.jpg", 10, 11, 100);
        derivativeList1.add(derivative11);
        MediaDerivative derivative12 = new MediaDerivative(1, 2, true, "image1_s.jpg", 30, 31, 300);
        derivativeList1.add(derivative12);
        mediaResult1.put(SQLMediaGetSproc.MEDIA_DERIVATIVES_SET, derivativeList1);

        Map<String, Object> mediaResult2 = new HashMap<>();
        List<Media> mediaList2 = new ArrayList<>();
        Media media2 = new Media(2, 2, "image2.jpg", false, 40, 41, 400, "test", new Date(), 400, 500, null);
        mediaList2.add(media2);
        mediaResult2.put(SQLMediaGetSproc.MEDIA_SET, mediaList2);
        List<MediaDerivative> derivativeList2 = new ArrayList<>();
        MediaDerivative derivative21 = new MediaDerivative(2, 1, true, "image2_t.jpg", 50, 51, 500);
        derivativeList2.add(derivative21);
        MediaDerivative derivative22 = new MediaDerivative(2, 2, true, "image2_s.jpg", 60, 61, 600);
        derivativeList2.add(derivative22);
        mediaResult2.put(SQLMediaGetSproc.MEDIA_DERIVATIVES_SET, derivativeList2);
        
        when(mediaSproc.execute(anyInt(), eq(mediaId1))).thenReturn(mediaResult1);
        when(mediaSproc.execute(anyInt(), eq(mediaId2))).thenReturn(mediaResult2);

        MediaDao mediaDao = new MediaDao(mediaIdSproc, mediaSproc);
        List<Media> testMediaList = mediaDao.getMediaByDomainId("Lodging", "1234", null, null);
    }

}
