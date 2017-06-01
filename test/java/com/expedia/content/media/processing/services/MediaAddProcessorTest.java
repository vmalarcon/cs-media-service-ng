package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.kafka.KafkaCommonPublisher;
import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ContextConfiguration(locations = "classpath:media-services.xml")
@RunWith(MockitoJUnitRunner.class)
public class MediaAddProcessorTest {
    private static final String REPROCESS_OPERATION = "reprocess";
    private static final String MEDIA_CLOUD_ROUTER_CLIENT_ID = "Media Cloud Router";


    @Mock
    private QueueMessagingTemplate messagingTemplate;
    @Mock
    private LogActivityProcess logActivityProcess;
    @Mock
    private Reporting reporting;
    @Mock
    private ThumbnailProcessor thumbnailProcessor;
    @Mock
    private MediaDao mockMediaDao;
    @Mock
    private KafkaCommonPublisher kafkaCommonPublisher;
    @Mock
    private Properties mockProviderProperties;

    private MediaAddProcessor mediaAddProcessor;
    private Properties providerProperties;

    @Before
    public void testSetUp() throws Exception {
        providerProperties = new Properties();
        providerProperties.setProperty("1", "EPC Internal User");
        providerProperties.setProperty("6", "SCORE");
        providerProperties.setProperty("3", "EPC Legacy");
        providerProperties.setProperty("53", "freetobook");
        providerProperties.setProperty("54", "Despegar");
        providerProperties.setProperty("56", "ICE Portal");
        providerProperties.setProperty("56", "VFMLeonardo");
        mediaAddProcessor = new MediaAddProcessor(mockMediaDao, kafkaCommonPublisher, thumbnailProcessor, logActivityProcess, reporting, messagingTemplate);
        setFieldValue(mediaAddProcessor, "providerProperties", mockProviderProperties);
        when(mockProviderProperties.entrySet()).thenReturn(providerProperties.entrySet());
    }

    @Test
    public void testProcessNewMediaAddRequest() throws Exception {
        String jsonMessage = "{ "
                + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", "
                + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", "
                + "\"domain\": \"Lodging\", "
                + "\"rotation\": \"90\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\" "
                + "}";
        when(mockMediaDao.getMediaByFilename("NASA_ISS-4.jpg")).thenReturn(Arrays.asList(Optional.empty()));
        mediaAddProcessor.processRequest(jsonMessage, "123", "expedia.com", "blinn", HttpStatus.ACCEPTED, new Date());
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mockMediaDao, times(1)).addMedia(argument.capture());
        verify(mockMediaDao, times(0)).updateMedia(any());
        verifyZeroInteractions(thumbnailProcessor);
        ImageMessage processedImageMessage = argument.getValue();
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(eq(processedImageMessage), anyString());
        assertNull(processedImageMessage.getOperation());
        assertEquals("1238_EPCInternalUser_" + processedImageMessage.getMediaGuid() + ".jpg", processedImageMessage.getFileName());
        assertEquals("http://i.imgur.com/3PRGFii.jpg", processedImageMessage.getFileUrl());
        assertEquals("bobthegreat", processedImageMessage.getUserId());
        assertEquals("90", processedImageMessage.getRotation());
    }

    @Test
    public void testProcessReprocessMediaAddRequest() throws Exception {
        String jsonMessage = "{ "
                + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", "
                + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", "
                + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\" "
                + "}";
        Optional<Media> originalMedia = Optional.of(Media.builder()
                .mediaGuid("12345678-1234-1234-1234-010203040506")
                .domainId("1238")
                .provider("EPC Internal User")
                .active("true")
                .lcmMediaId("123456")
                .fileName("NASA_ISS-4.jpg")
                .fileUrl("http://i.imgur.com/SOMETHINGDIFFERENT.jpg")
                .build());
        when(mockMediaDao.getMediaByFilename("NASA_ISS-4.jpg")).thenReturn(Arrays.asList(originalMedia));
        mediaAddProcessor.processRequest(jsonMessage, "123", "expedia.com", MEDIA_CLOUD_ROUTER_CLIENT_ID, HttpStatus.ACCEPTED, new Date());
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mockMediaDao, times(0)).addMedia(any());
        verify(mockMediaDao, times(1)).updateMedia(argument.capture());
        verifyZeroInteractions(thumbnailProcessor);
        ImageMessage processedImageMessage = argument.getValue();
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(eq(processedImageMessage), anyString());
        assertTrue(processedImageMessage.getOperation().contains(REPROCESS_OPERATION));
        assertEquals("12345678-1234-1234-1234-010203040506", processedImageMessage.getMediaGuid());
        assertEquals("NASA_ISS-4.jpg", processedImageMessage.getFileName());
        assertEquals("http://i.imgur.com/3PRGFii.jpg", processedImageMessage.getFileUrl());
        assertEquals("bobthegreat", processedImageMessage.getUserId());
    }

    @Test
    public void testProcessNewMediaAddFromMediaCloudRouterRequest() throws Exception {
        String jsonMessage = "{ "
                + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", "
                + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", "
                + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\" "
                + "}";
        when(mockMediaDao.getMediaByFilename("NASA_ISS-4.jpg")).thenReturn(Arrays.asList(Optional.empty()));
        mediaAddProcessor.processRequest(jsonMessage, "123", "expedia.com", MEDIA_CLOUD_ROUTER_CLIENT_ID, HttpStatus.ACCEPTED, new Date());
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mockMediaDao, times(1)).addMedia(argument.capture());
        verify(mockMediaDao, times(0)).updateMedia(any());
        verifyZeroInteractions(thumbnailProcessor);
        ImageMessage processedImageMessage = argument.getValue();
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(eq(processedImageMessage), anyString());
        assertNull(processedImageMessage.getOperation());
        assertEquals("1238_EPCInternalUser_" + processedImageMessage.getMediaGuid() + ".jpg", processedImageMessage.getFileName());
        assertEquals("http://i.imgur.com/3PRGFii.jpg", processedImageMessage.getFileUrl());
        assertEquals("bobthegreat", processedImageMessage.getUserId());
    }

    @Test
    public void testProcessNewMediaAddWithThumbnailRequest() throws Exception {
        String jsonMessage = "{ "
                + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", "
                + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", "
                + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\","
                + "\"generateThumbnail\": \"true\" "
                + "}";
        when(mockMediaDao.getMediaByFilename("NASA_ISS-4.jpg")).thenReturn(Arrays.asList(Optional.empty()));
        Thumbnail thumbnail = Thumbnail.builder().location("s3://somewhere/but/not/here").build();
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);
        mediaAddProcessor.processRequest(jsonMessage, "123", "expedia.com", "blinn", HttpStatus.ACCEPTED, new Date());
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mockMediaDao, times(1)).addMedia(argument.capture());
        verify(thumbnailProcessor, times(1)).createThumbnail(any());
        verify(mockMediaDao, times(0)).updateMedia(any());
        ImageMessage processedImageMessage = argument.getValue();
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(eq(processedImageMessage), anyString());
        assertNull(processedImageMessage.getOperation());
        assertEquals("1238_EPCInternalUser_" + processedImageMessage.getMediaGuid() + ".jpg", processedImageMessage.getFileName());
        assertEquals("http://i.imgur.com/3PRGFii.jpg", processedImageMessage.getFileUrl());
        assertEquals("bobthegreat", processedImageMessage.getUserId());
    }

    @Test
    public void testProcessNewMediaAddWithThumbnailErrorRequest() throws Exception {
        String jsonMessage = "{ "
                + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", "
                + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", "
                + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\","
                + "\"generateThumbnail\": \"true\" "
                + "}";
        when(mockMediaDao.getMediaByFilename("NASA_ISS-4.jpg")).thenReturn(Arrays.asList(Optional.empty()));
        when(thumbnailProcessor.createThumbnail(any(ImageMessage.class))).thenThrow(new RuntimeException("whoops"));
        ResponseEntity response = mediaAddProcessor.processRequest(jsonMessage, "123", "expedia.com", "blinn", HttpStatus.ACCEPTED, new Date());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }

}
