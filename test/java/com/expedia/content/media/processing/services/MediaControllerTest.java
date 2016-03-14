package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.LogEntry;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.services.dao.LcmDynamoMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.util.MediaReplacement;
import com.expedia.content.media.processing.services.validator.MapMessageValidator;
import com.google.common.collect.Lists;
import org.codehaus.plexus.util.ReflectionUtils;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.MultiValueMap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static com.expedia.content.media.processing.services.util.MediaReplacementTest.createByFileNameMedia;
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

@ContextConfiguration(locations = "classpath:media-services.xml")
@RunWith(MockitoJUnitRunner.class)
public class MediaControllerTest {

    private static final String TEST_CLIENT_ID = "a-user";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Mock
    private Reporting reporting;
    @Mock
    private QueueMessagingTemplate queueMessagingTemplateMock;
    @Mock
    Properties mockProviderProperties;

    private Set<Map.Entry<Object, Object>> providerMapping;
    private MediaController mediaController;

    @Before
    public void setSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(TEST_CLIENT_ID);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Before
    public void initialize() throws IllegalAccessException {
        mediaController = new MediaController();
        ReflectionUtils.setVariableValueInObject(mediaController, "providerProperties", mockProviderProperties);
        providerMapping = new HashSet<>();
        providerMapping.add(new org.apache.commons.collections4.keyvalue.DefaultMapEntry("1", "EPC Internal User"));
        providerMapping.add(new org.apache.commons.collections4.keyvalue.DefaultMapEntry("6", "SCORE"));
        providerMapping.add(new org.apache.commons.collections4.keyvalue.DefaultMapEntry("53", "freetobook"));
        providerMapping.add(new org.apache.commons.collections4.keyvalue.DefaultMapEntry("56", "ReplaceProvider"));
        when(mockProviderProperties.entrySet()).thenReturn(providerMapping);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testValidateImageSuccess() throws Exception {
        String jsonMessage =
                "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", " + "\"userId\": \"bobthegreat\", "
                        + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"EPC Internal User\" " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        setFieldValue(mediaController, "mediaDao", mock(LcmDynamoMediaDao.class));

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
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"userId\": \"bobthegreat\", " + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", " + "\"domainProvider\": \"EPC Internal User\" " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        setFieldValue(mediaController, "mediaDao", mock(LcmDynamoMediaDao.class));

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

    @Test
    public void testValidateImageSuccessWithThumbnail() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", " + "\"generateThumbnail\": \"true\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\" " + "}";

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
        setFieldValue(mediaController, "mediaDao", mock(LcmDynamoMediaDao.class));

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
        String jsonMessage =
                "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", " + "\"userId\": \"bobthegreat\", "
                        + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"EPC Internal User\" " + "}";

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

    @Test
    public void testURLNotFound() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgasdfasdfasdfur.com/3PRGFasdfasdfasdfii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"EPC Internal User\" "
                + "}";

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
        String jsonMessage = "{  \n" + "   \"mediaProviderId\":\"1001\",\n" + "   \"fileUrl\":\"http://images.com/dir1/img1.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n" + "   \"stagingKey\":{  \n" + "      \"externalId\":\"222\",\n" + "      \"providerId\":\"300\",\n"
                + "      \"sourceId\":\"99\"\n" + "   },\n" + "   \"expediaId\":\"NOT_A_NUMBER\",\n" + "   \"categoryId\":\"801\",\n"
                + "   \"callback\":\"http://multi.source.callback/callback\",\n" + "   \"caption\":\"caption\"\n" + "}";
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.acquireMedia(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaByDomainIdInvalidActiveFilter() throws Exception {
        mediaController = new MediaController();
        MultiValueMap<String, String> headers = new HttpHeaders();
        ResponseEntity<String> responseEntity = mediaController.getMediaByDomainId("Lodging", "1234", "potato", null, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaByDomainIdInvalidDomain() throws Exception {
        MultiValueMap<String, String> headers = new HttpHeaders();
        ResponseEntity<String> responseEntity = mediaController.getMediaByDomainId("potato", "1234", "true", null, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaByDomainIdLodging() throws Exception {

        List<String> commentList = new LinkedList<>();
        commentList.add("Comment1");
        commentList.add("Comment2");
        Map<String, Object> domainData = new HashMap<>();
        domainData.put("propertyHero", "true");
        Media mediaItem1 = Media.builder().active("true").domain("Lodging").domainId("1234").fileName("1234_file_name.jpg")
                .mediaGuid("102d3a4b-c985-43d3-9245-b60ab1eb9a0f").lastUpdated(new Date()).domainData(domainData).lcmMediaId("4321").build();
        Media mediaItem2 = Media.builder().active("true").domain("Lodging").domainId("1234").fileName("1234_file2_name.jpg")
                .mediaGuid("ea868d7d-c4ce-41a8-be43-19fff0ce5ad4").lastUpdated(new Date()).commentList(commentList).build();
        List<Media> mediaValues = new ArrayList<>();
        mediaValues.add(mediaItem1);
        mediaValues.add(mediaItem2);

        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByDomainId(any(), anyString(), anyString(), anyString())).thenReturn(mediaValues);

        setFieldValue(mediaController, "mediaDao", mockMediaDao);

        MultiValueMap<String, String> headers = new HttpHeaders();
        ResponseEntity<String> responseEntity = mediaController.getMediaByDomainId("Lodging", "1234", "true", null, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        String domainString = "\"domain\":\"Lodging\"";
        assertTrue(responseEntity.getBody().contains(domainString));
        assertEquals(responseEntity.getBody().indexOf(domainString), responseEntity.getBody().lastIndexOf(domainString));
        String domainIdString = "\"domainId\":\"1234\"";
        assertTrue(responseEntity.getBody().contains(domainIdString));
        assertEquals(responseEntity.getBody().indexOf(domainIdString), responseEntity.getBody().lastIndexOf(domainIdString));
        assertTrue(responseEntity.getBody().contains("\"images\":["));
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\":\"102d3a4b-c985-43d3-9245-b60ab1eb9a0f\""));
        assertTrue(responseEntity.getBody()
                .replaceAll("[0-9]{4}-[0-9]{2}-[0-9]{2}[\\s][0-9]{1,2}:[0-9]{2}:[0-9]{2}[\\.][0-9]+\\s{1}[\\+|\\-][0-9]{2}:[0-9]{2}", "timestamp")
                .contains("\"lastUpdateDateTime\":\"timestamp\""));
        assertTrue(responseEntity.getBody().contains("\"comments\":[{\"note\":\"Comment1\",\"timestamp\""));
        assertTrue(responseEntity.getBody().contains(",{\"note\":\"Comment2\",\"timestamp\""));
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\":\"ea868d7d-c4ce-41a8-be43-19fff0ce5ad4\""));
        assertTrue(responseEntity.getBody().contains("\"lcmMediaId\":\"4321\""));
        assertTrue(responseEntity.getBody().contains("\"domainFields\":{"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
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

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);

        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByFilename(eq("123_1_NASA_ISS-4.jpg"))).thenReturn(Lists.newArrayList(
                createByFileNameMedia("old-guid", "456", "true", DATE_FORMAT.parse("2016-02-17 12:00:00"),"456"),
                createByFileNameMedia("old-but-inactive", "567", "false", DATE_FORMAT.parse("2016-10-10 12:00:00"),"456"),
                createByFileNameMedia("too-old", "890", "true", DATE_FORMAT.parse("2016-02-17 11:59:59"),"456")
        ));
        setFieldValue(mediaController, "mediaDao", mockMediaDao);

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

    @SuppressWarnings({"unchecked", "rawtypes"})
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

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);

        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByFilename(eq("123_1_NASA_ISS-4.jpg"))).thenReturn(Lists.newArrayList());
        setFieldValue(mediaController, "mediaDao", mockMediaDao);

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testCaseInsensitiveMediaProvider() throws Exception {
        String jsonMessage =
                "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", " + "\"userId\": \"bobthegreat\", "
                        + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"ScoRE\" " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        setFieldValue(mediaController, "mediaDao", mock(LcmDynamoMediaDao.class));

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
        assertTrue(publishedMessageValue.getPayload().contains("\"domainProvider\":\"SCORE\""));
    }

    @Test
    public void testFreeToBookFileNameExtraction() throws Exception {
        String jsonMessage =
                "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg/why/would/someone/name/all/of/their/files/original.jpg\", " + "\"fileName\": \"original.jpg\", " + "\"userId\": \"bobthegreat\", "
                        + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"freetobook\" " + "}";
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        setFieldValue(mediaController, "mediaDao", mock(LcmDynamoMediaDao.class));

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
        assertTrue(publishedMessageValue.getPayload().matches("(.*)\"fileName\":\"1238_freetobook_(.*).jpg\"(.*)"));

    }

    @SuppressWarnings({"unchecked"})
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
}
