package com.expedia.content.media.processing.services;

import static com.expedia.content.media.processing.services.validator.MediaReplacementTest.createMedia;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.Media;
import com.expedia.content.media.processing.services.dao.MediaDAO;
import com.expedia.content.media.processing.services.validator.MediaReplacement;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;

import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.LogEntry;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.services.validator.MapMessageValidator;

@RunWith(MockitoJUnitRunner.class)
public class MediaControllerTest {

    private static final String TEST_CLIENT_ID = "a-user";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Mock
    private Reporting reporting;
    @Mock
    private QueueMessagingTemplate queueMessagingTemplateMock;

    private MediaReplacement mediaReplacement = new MediaReplacement("ReplaceProvider");

    @Before
    public void setSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(TEST_CLIENT_ID);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testValidateImageSuccess() throws Exception {
        String jsonMessage = "{ " +
                  "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " +
                  "\"fileName\": \"NASA_ISS-4.jpg\", " +
                  "\"userId\": \"bobthegreat\", " +
                  "\"domain\": \"Lodging\", " +
                  "\"domainId\": \"1238\", " +
                  "\"domainProvider\": \"EPC-Internal\" " +
                 "}";

        MediaController mediaController = new MediaController();

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        setFieldValue(mediaController, "mediaReplacement", mediaReplacement);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\""));
        assertFalse(responseEntity.getBody().contains("\"mediaGuid\":null"));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(1)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"NASA_ISS-4.jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + TEST_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\"" + requestId));
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testValidateImageSuccessWithoutFileName() throws Exception {
        String jsonMessage = "{ " +
                  "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " +
                  "\"userId\": \"bobthegreat\", " +
                  "\"domain\": \"Lodging\", " +
                  "\"domainId\": \"1238\", " +
                  "\"domainProvider\": \"EPC-Internal\" " +
                 "}";

        MediaController mediaController = new MediaController();

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        setFieldValue(mediaController, "mediaReplacement", mediaReplacement);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\""));
        assertFalse(responseEntity.getBody().contains("\"mediaGuid\":null"));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(1)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"3PRGFii.jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + TEST_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\"" + requestId));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testValidateImageSuccessWithThumbnail() throws Exception {
        String jsonMessage = "{ " +
                  "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " +
                  "\"fileName\": \"NASA_ISS-4.jpg\", " +
                  "\"userId\": \"bobthegreat\", " +
                  "\"generateThumbnail\": \"true\", " +
                  "\"domain\": \"Lodging\", " +
                  "\"domainId\": \"1238\", " +
                  "\"domainProvider\": \"EPC-Internal\" " +
                 "}";

        MediaController mediaController = new MediaController();

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        when(thumbnailProcessor.createThumbnail(anyString(), anyString(), anyString(), anyString())).thenReturn(thumbnailUrl);
        setFieldValue(mediaController, "thumbnailProcessor", thumbnailProcessor);
        
        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        setFieldValue(mediaController, "mediaReplacement", mediaReplacement);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\""));
        assertFalse(responseEntity.getBody().contains("\"mediaGuid\":null"));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));
        assertTrue(responseEntity.getBody().contains("\"thumbnailUrl\":\"" + thumbnailUrl + "\""));

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(1)).log(logEntryCaptor.capture(), eq(reporting));
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), any());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testClientNotValid() throws Exception {
        String jsonMessage = "{ " +
                  "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " +
                  "\"fileName\": \"NASA_ISS-4.jpg\", " +
                  "\"userId\": \"bobthegreat\", " +
                  "\"domain\": \"Lodging\", " +
                  "\"domainId\": \"1238\", " +
                  "\"domainProvider\": \"EPC-Internal\" " +
                 "}";

        MediaController mediaController = new MediaController();

        Map<String, List<MapMessageValidator>> validators = new HashMap<>();
        List<MapMessageValidator> messageValidator = new ArrayList<>();
        MapMessageValidator mockMessageValidator = mock(MapMessageValidator.class);
        List<Map<String, String>> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(anyList())).thenReturn(validationErrorList);
        messageValidator.add(mockMessageValidator);
        validators.put("b-user", messageValidator);
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(0)).log(logEntryCaptor.capture(), eq(reporting));
        verify(queueMessagingTemplateMock, times(0)).send(anyString(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testURLNotFound() throws Exception {
        String jsonMessage = "{ " +
                  "\"fileUrl\": \"http://i.imgasdfasdfasdfur.com/3PRGFasdfasdfasdfii.jpg\", " +
                  "\"fileName\": \"NASA_ISS-4.jpg\", " +
                  "\"userId\": \"bobthegreat\", " +
                  "\"domain\": \"Lodging\", " +
                  "\"domainId\": \"1238\", " +
                  "\"domainProvider\": \"EPC-Internal\" " +
                 "}";

        MediaController mediaController = new MediaController();

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(0)).log(logEntryCaptor.capture(), eq(reporting));
        verify(queueMessagingTemplateMock, times(0)).send(anyString(), any());
    }
    
    @Test
    public void testAlphanumericStringExpediaIdInJsonMessage() throws Exception {
        String jsonMessage = "{  \n" +
                "   \"mediaProviderId\":\"1001\",\n" +
                "   \"fileUrl\":\"http://images.com/dir1/img1.jpg\",\n" +
                "   \"imageType\":\"Lodging\",\n" +
                "   \"stagingKey\":{  \n" +
                "      \"externalId\":\"222\",\n" +
                "      \"providerId\":\"300\",\n" +
                "      \"sourceId\":\"99\"\n" +
                "   },\n" +
                "   \"expediaId\":\"NOT_A_NUMBER\",\n" +
                "   \"categoryId\":\"801\",\n" +
                "   \"callback\":\"http://multi.source.callback/callback\",\n" +
                "   \"caption\":\"caption\"\n" +
                "}";
        MediaController mediaController = new MediaController();
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.acquireMedia(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testReplaceImageSuccess() throws Exception {
        String jsonMessage = "{ " +
                "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " +
                "\"fileName\": \"123_1_NASA_ISS-4.jpg\", " +
                "\"userId\": \"bobthegreat\", " +
                "\"domain\": \"Lodging\", " +
                "\"domainId\": \"1238\", " +
                "\"domainProvider\": \"ReplaceProvider\", " +
                "\"domainFields\": { " +
                "    \"replace\": \"true\" " +
                "  } " +
                "}";

        MediaController mediaController = new MediaController();

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        setFieldValue(mediaController, "mediaReplacement", mediaReplacement);

        MediaDAO mockMediaDao = mock(MediaDAO.class);
        when(mockMediaDao.getMediaByFilename(eq("123_1_NASA_ISS-4.jpg"))).thenReturn(Lists.newArrayList(
                createMedia("old-guid", "456", "true", DATE_FORMAT.parse("2016-02-17 12:00:00")),
                createMedia("old-but-inactive", "567", "false", DATE_FORMAT.parse("2016-10-10 12:00:00")),
                createMedia("too-old", "890", "true", DATE_FORMAT.parse("2016-02-17 11:59:59"))
        ));
        setFieldValue(mediaController, "mediaDAO", mockMediaDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\":\"old-guid\""));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(1)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"123_1_NASA_ISS-4.jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + TEST_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\"" + requestId));
        assertTrue(publishedMessageValue.getPayload().contains("\"mediaGuid\":\"" + "old-guid"));
        assertTrue(publishedMessageValue.getPayload().contains("\"lcmMediaId\":\"" + "456"));
    }

    @Test
    public void testReplaceImageButNoOldFound() throws Exception {
        String jsonMessage = "{ " +
                "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " +
                "\"fileName\": \"123_1_NASA_ISS-4.jpg\", " +
                "\"userId\": \"bobthegreat\", " +
                "\"domain\": \"Lodging\", " +
                "\"domainId\": \"1238\", " +
                "\"domainProvider\": \"ReplaceProvider\", " +
                "\"domainFields\": { " +
                "    \"replace\": \"true\" " +
                "  } " +
                "}";

        MediaController mediaController = new MediaController();

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        setFieldValue(mediaController, "mediaReplacement", mediaReplacement);

        MediaDAO mockMediaDao = mock(MediaDAO.class);
        when(mockMediaDao.getMediaByFilename(eq("123_1_NASA_ISS-4.jpg"))).thenReturn(Lists.newArrayList());
        setFieldValue(mediaController, "mediaDAO", mockMediaDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\":"));
        ImageMessage response = ImageMessage.parseJsonMessage(responseEntity.getBody());
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(1)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"123_1_NASA_ISS-4.jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + TEST_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\"" + requestId));
        assertTrue(publishedMessageValue.getPayload().contains("\"mediaGuid\":\"" + response.getMediaGuid() + "\""));
        assertFalse(publishedMessageValue.getPayload().contains("\"lcmMediaId\":"));
    }

    private static Map<String, List<MapMessageValidator>> getMockValidators() {
        Map<String, List<MapMessageValidator>> validators = new HashMap<>();
        List<MapMessageValidator> messageValidator = new ArrayList<>();
        MapMessageValidator mockMessageValidator = mock(MapMessageValidator.class);
        List<Map<String, String>> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(anyList())).thenReturn(validationErrorList);
        messageValidator.add(mockMessageValidator);
        validators.put(TEST_CLIENT_ID, messageValidator);
        return validators;
    }

    private static void setFieldValue(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        final Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
