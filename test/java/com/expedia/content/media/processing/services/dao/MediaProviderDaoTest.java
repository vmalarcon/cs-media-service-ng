package com.expedia.content.media.processing.services.dao;


import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MediaProviderDaoTest {

    @BeforeClass
    public static void setUp() {
        System.setProperty("EXPEDIA_ENVIRONMENT", "test");
        System.setProperty("AWS_REGION", "us-west-2");
    }

    @Test
    public void testMediaProviderIdExists() throws Exception {
        List<MediaProvider> mediaProviders = new ArrayList<>();
        MediaProvider mediaProvider = new MediaProvider(1, "EPC Internal User", new Timestamp(1339150200000L),
                "phoenix", null);
        mediaProviders.add(mediaProvider);
        MediaProviderSproc mockMediaProviderSproc = mock(MediaProviderSproc.class);
        MediaProviderDao mediaProviderDao = new MediaProviderDao(mockMediaProviderSproc);
        Map<String, Object> mockResults = new HashMap<>();
        mockResults.put(MediaProviderSproc.MEDIA_PROVIDER_MAPPER_RESULT_SET, mediaProviders);
        when(mockMediaProviderSproc.execute()).thenReturn(mockResults);
        assertTrue(mediaProviderDao.getMediaProviderList(1, null));
    }

    @Test
    public void testMediaProviderNameExists() throws Exception {
        List<MediaProvider> mediaProviders = new ArrayList<>();
        MediaProvider mediaProvider = new MediaProvider(1, "EPC Internal User", new Timestamp(1339150200000L),
                "phoenix", null);
        mediaProviders.add(mediaProvider);
        MediaProviderSproc mockMediaProviderSproc = mock(MediaProviderSproc.class);
        MediaProviderDao mediaProviderDao = new MediaProviderDao(mockMediaProviderSproc);
        Map<String, Object> mockResults = new HashMap<>();
        mockResults.put(MediaProviderSproc.MEDIA_PROVIDER_MAPPER_RESULT_SET, mediaProviders);
        when(mockMediaProviderSproc.execute()).thenReturn(mockResults);
        assertTrue(mediaProviderDao.getMediaProviderList(null, "EPC Internal User"));
        verify(mockMediaProviderSproc.execute(), times(1));
    }

    @Test
    public void testMediaProviderIdDoesNotExist() throws Exception {
        List<MediaProvider> mediaProviders = new ArrayList<>();
        MediaProvider mediaProvider = new MediaProvider(1, "EPC Internal User", new Timestamp(1339150200000L),
                "phoenix", null);
        mediaProviders.add(mediaProvider);
        MediaProviderSproc mockMediaProviderSproc = mock(MediaProviderSproc.class);
        MediaProviderDao mediaProviderDao = new MediaProviderDao(mockMediaProviderSproc);
        Map<String, Object> mockResults = new HashMap<>();
        mockResults.put(MediaProviderSproc.MEDIA_PROVIDER_MAPPER_RESULT_SET, mediaProviders);
        when(mockMediaProviderSproc.execute()).thenReturn(mockResults);
        assertFalse(mediaProviderDao.getMediaProviderList(2, null));
        verify(mockMediaProviderSproc.execute(), times(1));
    }

    @Test
    public void testMediaProviderNameDoesNotExist() throws Exception {
        List<MediaProvider> mediaProviders = new ArrayList<>();
        MediaProvider mediaProvider = new MediaProvider(1, "EPC Internal User", new Timestamp(1339150200000L),
                "phoenix", null);
        mediaProviders.add(mediaProvider);
        MediaProviderSproc mockMediaProviderSproc = mock(MediaProviderSproc.class);
        MediaProviderDao mediaProviderDao = new MediaProviderDao(mockMediaProviderSproc);
        Map<String, Object> mockResults = new HashMap<>();
        mockResults.put(MediaProviderSproc.MEDIA_PROVIDER_MAPPER_RESULT_SET, mediaProviders);
        when(mockMediaProviderSproc.execute()).thenReturn(mockResults);
        assertFalse(mediaProviderDao.getMediaProviderList(null, "does not exist"));
        verify(mockMediaProviderSproc.execute(), times(1));
    }
}
