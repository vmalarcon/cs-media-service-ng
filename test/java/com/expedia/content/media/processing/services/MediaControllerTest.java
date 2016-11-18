package com.expedia.content.media.processing.services;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static com.expedia.content.media.processing.services.util.MediaReplacementTest.createByFileNameMedia;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.codehaus.plexus.util.ReflectionUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
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

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.reporting.Activity;
import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.LogEntry;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.dao.CatalogItemMediaDao;
import com.expedia.content.media.processing.services.dao.LcmDynamoMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.MediaUpdateDao;
import com.expedia.content.media.processing.services.dao.SKUGroupCatalogItemDao;
import com.expedia.content.media.processing.services.dao.domain.LcmCatalogItemMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaAndDerivative;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaChgSproc;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.MediaLstWithCatalogItemMediaAndMediaFileNameSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaListSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLRoomGetByCatalogItemIdSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLRoomGetSproc;
import com.expedia.content.media.processing.services.reqres.Comment;
import com.expedia.content.media.processing.services.reqres.DomainIdMedia;
import com.expedia.content.media.processing.services.reqres.MediaByDomainIdResponse;
import com.expedia.content.media.processing.services.reqres.MediaGetResponse;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.validator.MapMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import com.google.common.collect.Lists;

@ContextConfiguration(locations = "classpath:media-services.xml")
@RunWith(MockitoJUnitRunner.class)
public class MediaControllerTest {

    private static final String TEST_CLIENT_ID = "a-user";
    private static final String EPS_MEDIA_API_CLIENT_ID = "EPC Media API";
    private static final String MEDIA_CLOUD_ROUTER_CLIENT_ID = "Media Cloud Router";
    private static final String MULTISOURCE = "Multisource";
    private static final String RESPONSE_FIELD_LCM_MEDIA_ID = "lcmMediaId";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS ZZ");

    @Mock
    private Reporting reporting;
    @Mock
    private QueueMessagingTemplate queueMessagingTemplateMock;
    @Mock
    private DynamoMediaRepository dynamoMediaRepository;
    @Mock
    Properties mockProviderProperties;
    @Mock
    private KafkaPublisher kafkaPublisher;

    private Set<Map.Entry<Object, Object>> providerMapping;
    private MediaController mediaController;
    private MediaController mediaControllerSpy;

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
        providerMapping.add(new org.apache.commons.collections4.keyvalue.DefaultMapEntry("3", "EPC Legacy"));
        providerMapping.add(new org.apache.commons.collections4.keyvalue.DefaultMapEntry("53", "freetobook"));
        providerMapping.add(new org.apache.commons.collections4.keyvalue.DefaultMapEntry("54", "Despegar"));
        providerMapping.add(new org.apache.commons.collections4.keyvalue.DefaultMapEntry("56", "ICE Portal"));
        providerMapping.add(new org.apache.commons.collections4.keyvalue.DefaultMapEntry("56", "VFMLeonardo"));
        when(mockProviderProperties.entrySet()).thenReturn(providerMapping);
        DynamoMediaRepository dynamoMediaRepository = mock(DynamoMediaRepository.class);
        Mockito.doNothing().when(dynamoMediaRepository).storeMediaAddMessage(anyObject(), anyObject());
        FieldUtils.writeField(mediaController, "dynamoMediaRepository", dynamoMediaRepository, true);
        FieldUtils.writeField(mediaController, "kafkaPublisher", kafkaPublisher, true);
        Mockito.doNothing().when(kafkaPublisher).publishToTopic(anyObject());
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
        LcmDynamoMediaDao mockLcmDynamoMediaDao = mock(LcmDynamoMediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockLcmDynamoMediaDao);
        setFieldValue(mediaController, "dynamoMediaRepository", mock(DynamoMediaRepository.class));

        String requestId = "test-request-id"; // invalid UUID provided
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\""));
        assertFalse(responseEntity.getBody().contains("\"mediaGuid\":null"));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));
        ImageMessage response = ImageMessage.parseJsonMessage(responseEntity.getBody());

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"1238_EPCInternalUser_" + response.getMediaGuid() + ".jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"providedName\":\"NASA_ISS-4.jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + TEST_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\""));
        assertFalse(publishedMessageValue.getPayload().contains("test-request-id"));
        assertTrue(publishedMessageValue.getPayload().contains("\"logEntries\":[{\"activity\":\"" + Activity.RECEPTION.getName() + "\""
                + ",\"appName\":\"cs-media-service\",\"activityTime\":" ));
        assertTrue(publishedMessageValue.getPayload().contains("\"activity\":\"" + Activity.MEDIA_MESSAGE_RECEIVED.getName() + "\""
                + ",\"appName\":\"cs-media-service\",\"activityTime\":" ));
        verifyZeroInteractions(mockLcmDynamoMediaDao);
        ArgumentCaptor<ImageMessage> imageMessage = ArgumentCaptor.forClass(ImageMessage.class);
        verify(kafkaPublisher, times(1)).publishToTopic(imageMessage.capture());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testValidateImageSuccessWithoutFileName() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"userId\": \"bobthegreat\", " + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", " + "\"domainProvider\": \"EPC Legacy\" " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        LcmDynamoMediaDao mockLcmDynamoMediaDao = mock(LcmDynamoMediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockLcmDynamoMediaDao);
        setFieldValue(mediaController, "dynamoMediaRepository", mock(DynamoMediaRepository.class));

        String requestId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\""));
        assertFalse(responseEntity.getBody().contains("\"mediaGuid\":null"));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));
        ImageMessage response = ImageMessage.parseJsonMessage(responseEntity.getBody());

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"1238_EPCLegacy_" + response.getMediaGuid() + ".jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"providedName\":\"3PRGFii.jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + TEST_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\"" + requestId));
        verifyZeroInteractions(mockLcmDynamoMediaDao);
    }

    @Test
    public void testValidateImageSuccessWithThumbnail() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", " + "\"generateThumbnail\": \"true\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Legacy\" " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);
        setFieldValue(mediaController, "thumbnailProcessor", thumbnailProcessor);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        LcmDynamoMediaDao mockLcmDynamoMediaDao = mock(LcmDynamoMediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockLcmDynamoMediaDao);
        setFieldValue(mediaController, "dynamoMediaRepository", mock(DynamoMediaRepository.class));

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
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), any());
        verifyZeroInteractions(mockLcmDynamoMediaDao);
    }

    @Test
    public void testValidateImageSuccessWithThumbnailAndRotation() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"rotation\": \"90\", " + "\"userId\": \"bobthegreat\", " + "\"generateThumbnail\": \"true\", " + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\" " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);
        setFieldValue(mediaController, "thumbnailProcessor", thumbnailProcessor);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        setFieldValue(mediaController, "mediaDao", mock(LcmDynamoMediaDao.class));
        setFieldValue(mediaController, "dynamoMediaRepository", mock(DynamoMediaRepository.class));

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
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testClientNotValid() throws Exception {
        String jsonMessage =
                "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", " + "\"userId\": \"bobthegreat\", "
                        + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"EPC Legacy\" " + "}";

        Map<String, List<MapMessageValidator>> validators = new HashMap<>();
        List<MapMessageValidator> messageValidator = new ArrayList<>();
        MapMessageValidator mockMessageValidator = mock(MapMessageValidator.class);
        List<String> validationErrorList = new ArrayList<>();
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

        verifyZeroInteractions(mockLogActivityProcess);
        verify(queueMessagingTemplateMock, times(0)).send(anyString(), any());
    }

    @Test
    public void testURLNotFound() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgasdfasdfasdfur.com/3PRGFasdfasdfasdfii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"EPC Legacy\" "
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

        verifyZeroInteractions(mockLogActivityProcess);
        verify(queueMessagingTemplateMock, times(0)).send(anyString(), any());
    }

    @Test
    public void testAlphanumericStringExpediaIdInJsonMessage() throws Exception {
        String jsonMessage = "{  \n" + "   \"mediaProviderId\":\"1001\",\n" + "   \"fileUrl\":\"http://images.com/dir1/img1.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n" + "   \"stagingKey\":{  \n" + "      \"externalId\":\"222\",\n" + "      \"providerId\":\"300\",\n"
                + "      \"sourceId\":\"99\"\n" + "   },\n" + "   \"expediaId\":\"NOT_A_NUMBER\",\n" + "   \"subcategoryId\":\"801\",\n"
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

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);
        ResponseEntity<String> responseEntity = mediaController.getMediaByDomainId("Lodging", "1234", null, null, "potato", null, null, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaByDomainIdInvalidDomain() throws Exception {
        MultiValueMap<String, String> headers = new HttpHeaders();

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);
        ResponseEntity<String> responseEntity = mediaController.getMediaByDomainId("potato", "1234", null, null, "true", null, null, headers);
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

        Media mediaItem1 = Media.builder()
                .active("true")
                .domain("Lodging")
                .domainId("1234")
                .fileName("1234_file_name.jpg")
                .sourceUrl("http://some-source-url-1")
                .mediaGuid("102d3a4b-c985-43d3-9245-b60ab1eb9a0f")
                .lastUpdated(new Date())
                .domainData(domainData)
                .lcmMediaId("4321")
                .build();

        Media mediaItem2 = Media.builder()
                .active("true")
                .domain("Lodging")
                .domainId("1234")
                .fileName("1234_file2_name.jpg")
                .sourceUrl("http://some-source-url-2")
                .mediaGuid("ea868d7d-c4ce-41a8-be43-19fff0ce5ad4")
                .lastUpdated(new Date())
                .commentList(commentList)
                .build();

        List<Media> mediaValues = new ArrayList<>();
        mediaValues.add(mediaItem1);
        mediaValues.add(mediaItem2);

        MediaByDomainIdResponse response = MediaByDomainIdResponse.builder()
                .domain("Lodging")
                .domainId("1234")
                .totalMediaCount(mediaValues.size())
                .images(transformMediaListForResponse(mediaValues))
                .build();

        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByDomainId(any(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(response);
        SKUGroupCatalogItemDao skuGroupCatalogItemDao = mock(SKUGroupCatalogItemDao.class);
        when(skuGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(true);

        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        setFieldValue(mediaController, "skuGroupCatalogItemDao", skuGroupCatalogItemDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        MultiValueMap<String, String> headers = new HttpHeaders();
        ResponseEntity<String> responseEntity = mediaController.getMediaByDomainId("Lodging", "1234", null, null, "true", null, null, headers);
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


        Map<String, Object> jsonMap = JSONUtil.buildMapFromJson(responseEntity.getBody());
        Map map1 =(HashMap)((List) jsonMap.get("images")).get(0);
        assertTrue(map1.get("mediaGuid").equals("102d3a4b-c985-43d3-9245-b60ab1eb9a0f"));
        assertTrue(map1.get("sourceUrl").equals("http://some-source-url-1"));
        Map map2 =(HashMap)((List) jsonMap.get("images")).get(1);
        assertTrue(map2.get("mediaGuid").equals("ea868d7d-c4ce-41a8-be43-19fff0ce5ad4"));
        assertTrue(map2.get("sourceUrl").equals("http://some-source-url-2"));
    }

    @Test
    public void testMediaByDomainIdLodgingSortBySubCategoryID() throws Exception {

        List<String> commentList = new LinkedList<>();
        commentList.add("Comment1");
        commentList.add("Comment2");
        Map<String, Object> domainData = new HashMap<>();
        domainData.put("propertyHero", "true");
        domainData.put("subcategoryId", "20001");

        Map<String, Object> domainData2 = new HashMap<>();
        domainData.put("propertyHero", "false");
        domainData.put("subcategoryId", "20001");

        Map<String, Object> domainData3 = new HashMap<>();
        domainData.put("propertyHero", "false");
        domainData.put("subcategoryId", "10001");


        Media mediaItem1 = Media.builder()
                .active("true")
                .domain("Lodging")
                .domainId("1234")
                .fileName("1234_file_name.jpg")
                .sourceUrl("http://some-source-url-1")
                .mediaGuid("first3a4b-c985-43d3-9245-b60ab1eb9a0f")
                .lastUpdated(new Date(new Date().getTime()+5000))
                .domainData(domainData)
                .lcmMediaId("4321")
                .build();

        Media mediaItem2 = Media.builder()
                .active("true")
                .domain("Lodging")
                .domainId("1234")
                .fileName("1234_file2_name.jpg")
                .sourceUrl("http://some-source-url-2")
                .mediaGuid("ea868d7d-c4ce-41a8-be43-19fff0ce5ad4")
                .lastUpdated(new Date(new Date().getTime()+100000))
                .domainData(domainData2)
                .commentList(commentList)
                .build();

        Media mediaItem3 = Media.builder()
                .active("true")
                .domain("Lodging")
                .domainId("1234")
                .fileName("1234_file2_name.jpg")
                .sourceUrl("http://some-source-url-3")
                .mediaGuid("last8d7d-c4ce-41a8-be43-19fff0ce5ad4")
                .lastUpdated(new Date())
                .domainData(domainData3)
                .commentList(commentList)
                .build();

        List<Media> mediaValues = new ArrayList<>();
        mediaValues.add(mediaItem1);
        mediaValues.add(mediaItem2);
        mediaValues.add(mediaItem3);


        MediaByDomainIdResponse response = MediaByDomainIdResponse.builder()
                .domain("Lodging")
                .domainId("1234")
                .totalMediaCount(mediaValues.size())
                .images(transformMediaListForResponse(mediaValues))
                .build();

        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByDomainId(any(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(response);
        SKUGroupCatalogItemDao skuGroupCatalogItemDao = mock(SKUGroupCatalogItemDao.class);
        when(skuGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(true);

        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        setFieldValue(mediaController, "skuGroupCatalogItemDao", skuGroupCatalogItemDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        MultiValueMap<String, String> headers = new HttpHeaders();
        ResponseEntity<String> responseEntity = mediaController.getMediaByDomainId("Lodging", "1234", null, null, "true", null, null, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Map<String, Object> jsonMap = JSONUtil.buildMapFromJson(responseEntity.getBody());
        Map map1 =(HashMap)((List) jsonMap.get("images")).get(0);
        assertTrue(map1.get("mediaGuid").equals("first3a4b-c985-43d3-9245-b60ab1eb9a0f"));
        assertTrue(map1.get("sourceUrl").equals("http://some-source-url-1"));
        Map map2 =(HashMap)((List) jsonMap.get("images")).get(1);
        assertTrue(map2.get("mediaGuid").equals("ea868d7d-c4ce-41a8-be43-19fff0ce5ad4"));
        assertTrue(map2.get("sourceUrl").equals("http://some-source-url-2"));
        Map map3 =(HashMap)((List) jsonMap.get("images")).get(2);
        assertTrue(map3.get("mediaGuid").equals("last8d7d-c4ce-41a8-be43-19fff0ce5ad4"));
        assertTrue(map3.get("sourceUrl").equals("http://some-source-url-3"));
    }

    @Test
    public void testMediaByDomainIdLodgingNotFound() throws Exception {
        SKUGroupCatalogItemDao skuGroupCatalogItemDao = mock(SKUGroupCatalogItemDao.class);
        when(skuGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(false);

        setFieldValue(mediaController, "skuGroupCatalogItemDao", skuGroupCatalogItemDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        MultiValueMap<String, String> headers = new HttpHeaders();
        ResponseEntity<String> responseEntity = mediaController.getMediaByDomainId("Lodging", "1234", null, null, "true", null, null, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals("DomainId not found: 1234", responseEntity.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testReplaceImageSuccess() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(MEDIA_CLOUD_ROUTER_CLIENT_ID);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"123_1_NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"SCORE\", "
                + "\"domainFields\": { " + "    \"replace\": \"true\" " + "  } " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);

        // Inactive images should be considered. 2016-10-10 is more recent than the others
        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByFilename(eq("123_1_NASA_ISS-4.jpg")))
                .thenReturn(Lists.newArrayList(createByFileNameMedia("old-guid", "1238", "false", DATE_FORMAT.parse("2016-02-17 12:00:00"), "4561"),
                        createByFileNameMedia("recent-but-inactive", "1238", "true", DATE_FORMAT.parse("2016-10-10 12:00:00"), "456"),
                        createByFileNameMedia("too-old", "1238", "true", DATE_FORMAT.parse("2016-02-17 11:59:59"), "4562")));
        setFieldValue(mediaController, "mediaDao", mockMediaDao);

        String requestId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\":\"recent-but-inactive\""));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"123_1_NASA_ISS-4.jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + MEDIA_CLOUD_ROUTER_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\"" + requestId));
        assertTrue(publishedMessageValue.getPayload().contains("\"mediaGuid\":\"" + "recent-but-inactive"));
        assertTrue(publishedMessageValue.getPayload().contains("\"lcmMediaId\":\"" + "456"));
        verify(mockMediaDao, times(1)).getMediaByFilename(eq("123_1_NASA_ISS-4.jpg"));
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testReplaceImageWhenDynamoNotFound() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(MEDIA_CLOUD_ROUTER_CLIENT_ID);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"123_1_NASA_ISS-4.jpg\", "+ "\"mediaGuid\": \"newGuid\", "
                + "\"userId\": \"bobthegreat\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"SCORE\", "
                + "\"domainFields\": { " + "    \"replace\": \"true\" " + "  } " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);

        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByFilename(eq("123_1_NASA_ISS-4.jpg")))
                .thenReturn(Lists.newArrayList(createByFileNameMedia("old-but-inactive", "1238", "false", DATE_FORMAT.parse("2016-10-10 12:00:00"), "456")));
        LcmMedia lcmMedia = LcmMedia.builder().mediaId(4567).active(true).build();

        when(mockMediaDao.getMediaByFilenameInLCM(eq(1238), eq("123_1_NASA_ISS-4.jpg")))
                .thenReturn(Lists.newArrayList(lcmMedia));
        setFieldValue(mediaController, "mediaDao", mockMediaDao);

        String requestId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"123_1_NASA_ISS-4.jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + MEDIA_CLOUD_ROUTER_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\"" + requestId));
        assertTrue(publishedMessageValue.getPayload().contains("\"lcmMediaId\":\"" + "456"));
        verify(mockMediaDao, times(1)).getMediaByFilename(eq("123_1_NASA_ISS-4.jpg"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testReplaceImageButNoOldFound() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(MEDIA_CLOUD_ROUTER_CLIENT_ID);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"123_1_NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"VFMLeonardo\", "
                + "\"domainFields\": { " + "    \"replace\": \"true\" " + "  } " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);

        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByFilename(eq("123_1_NASA_ISS-4.jpg"))).thenReturn(Lists.newArrayList());
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        setFieldValue(mediaController, "dynamoMediaRepository", mock(DynamoMediaRepository.class));

        String requestId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\":"));
        ImageMessage response = ImageMessage.parseJsonMessage(responseEntity.getBody());
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"123_1_NASA_ISS-4.jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + MEDIA_CLOUD_ROUTER_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\"" + requestId));
        assertTrue(publishedMessageValue.getPayload().contains("\"mediaGuid\":\"" + response.getMediaGuid() + "\""));
        assertFalse(publishedMessageValue.getPayload().contains("\"lcmMediaId\":"));
        verify(mockMediaDao, times(1)).getMediaByFilename(eq("123_1_NASA_ISS-4.jpg"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testCaseInsensitiveMediaProvider() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"ScoRE\" " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        LcmDynamoMediaDao mockLcmDynamoMediaDao = mock(LcmDynamoMediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockLcmDynamoMediaDao);
        setFieldValue(mediaController, "dynamoMediaRepository", mock(DynamoMediaRepository.class));

        String requestId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\""));
        assertFalse(responseEntity.getBody().contains("\"mediaGuid\":null"));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));
        ImageMessage response = ImageMessage.parseJsonMessage(responseEntity.getBody());

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"1238_SCORE_" + response.getMediaGuid() + ".jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + TEST_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\"" + requestId));
        assertTrue(publishedMessageValue.getPayload().contains("\"domainProvider\":\"SCORE\""));
        verifyZeroInteractions(mockLcmDynamoMediaDao);
    }

    @Test
    public void testFreeToBookFileNameExtraction() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg/why/would/someone/name/all/of/their/files/original.jpg\", "
                + "\"fileName\": \"original.jpg\", " + "\"userId\": \"bobthegreat\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"freetobook\" " + "}";
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        LcmDynamoMediaDao mockLcmDynamoMediaDao = mock(LcmDynamoMediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockLcmDynamoMediaDao);
        setFieldValue(mediaController, "dynamoMediaRepository", mock(DynamoMediaRepository.class));

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\""));
        assertFalse(responseEntity.getBody().contains("\"mediaGuid\":null"));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));
        ImageMessage response = ImageMessage.parseJsonMessage(responseEntity.getBody());

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"1238_freetobook_" + response.getMediaGuid() + ".jpg\""));
        verifyZeroInteractions(mockLcmDynamoMediaDao);
    }

    @Test
    public void testDespegarFileNameExtraction() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg/why/would/someone/name/all/of/their/files/original.jpg\", "
                + "\"fileName\": \"original.jpg\", " + "\"userId\": \"bobthegreat\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"Despegar\" " + "}";
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        LcmDynamoMediaDao mockLcmDynamoMediaDao = mock(LcmDynamoMediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockLcmDynamoMediaDao);
        setFieldValue(mediaController, "dynamoMediaRepository", mock(DynamoMediaRepository.class));

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\""));
        assertFalse(responseEntity.getBody().contains("\"mediaGuid\":null"));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));
        ImageMessage response = ImageMessage.parseJsonMessage(responseEntity.getBody());

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"1238_Despegar_" + response.getMediaGuid() + ".jpg\""));
        verifyZeroInteractions(mockLcmDynamoMediaDao);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testEPCFileFormat() throws Exception {
        String jsonMessage =
                "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", " + "\"userId\": \"bobthegreat\", "
                        + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"EPC Internal User\" }";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        LcmDynamoMediaDao mockLcmDynamoMediaDao = mock(LcmDynamoMediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockLcmDynamoMediaDao);
        setFieldValue(mediaController, "dynamoMediaRepository", mock(DynamoMediaRepository.class));

        String requestId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        mediaControllerSpy = spy(mediaController);
        when(mediaControllerSpy.verifyUrl(anyString())).thenReturn(new ValidationStatus(Boolean.TRUE, "validated", "valid"));
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\""));
        assertFalse(responseEntity.getBody().contains("\"mediaGuid\":null"));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));
        ImageMessage response = ImageMessage.parseJsonMessage(responseEntity.getBody());

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"1238_EPCInternalUser_" + response.getMediaGuid() + ".jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"providedName\":\"NASA_ISS-4.jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + TEST_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\"" + requestId));
        verifyZeroInteractions(mockLcmDynamoMediaDao);
    }

    @Test
    public void testValidateImageSuccessWithoutThumbnail() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", " + "\"generateThumbnail\": \"false\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Legacy\" " + "}";

        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);
        setFieldValue(mediaController, "thumbnailProcessor", thumbnailProcessor);

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        LcmDynamoMediaDao mockLcmDynamoMediaDao = mock(LcmDynamoMediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockLcmDynamoMediaDao);
        setFieldValue(mediaController, "dynamoMediaRepository", mock(DynamoMediaRepository.class));

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\""));
        assertFalse(responseEntity.getBody().contains("\"mediaGuid\":null"));
        assertTrue(responseEntity.getBody().contains("\"status\":\"RECEIVED\""));
        assertFalse(responseEntity.getBody().contains("\"thumbnailUrl\":\"" + thumbnailUrl + "\""));

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), any());
        verifyZeroInteractions(mockLcmDynamoMediaDao);
    }

    @Test
    public void testGetMediaByGUIDInvalid() throws Exception {
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        ResponseEntity<String> responseEntity = mediaController.getMedia("hello potato", mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testGetMediaByGUIDNotFound() throws Exception {
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByGUID(anyString())).thenReturn(null);
        setFieldValue(mediaController, "mediaDao", mock(MediaDao.class));
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        ResponseEntity<String> responseEntity = mediaController.getMedia("d2d4d480-9627-47f9-86c6-1874c18d37f4", mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Deprecated
    @Test
    public void testGetMediaByGUIDWithLcmId() throws Exception {
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        Media resultMedia = Media.builder()
                .active("true")
                .domain("Lodging")
                .domainId("1234")
                .fileName("47474_freetotbook_5h5h5h5h5h5h.jpg")
                .sourceUrl("http://some-source-url")
                .lcmMediaId("123456")
                .lastUpdated(new Date())
                .lcmMediaId("123456")
                .build();

        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByGUID(anyString())).thenReturn(transformSingleMediaForResponse(resultMedia));
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        ResponseEntity<String> responseEntity = mediaController.getMedia("123456", mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"domain\":\"Lodging\""));
        assertTrue(responseEntity.getBody().contains("\"domainId\":\"" + resultMedia.getDomainId() + "\""));
        assertTrue(responseEntity.getBody().contains("\"domainFields\":{"));
        assertTrue(responseEntity.getBody().contains("\"lcmMediaId\":\"" + resultMedia.getLcmMediaId() + "\""));
        assertTrue(responseEntity.getBody().contains("\"fileName\":\"" + resultMedia.getFileName() + "\""));
        assertTrue(responseEntity.getBody().contains("\"sourceUrl\":\"" + resultMedia.getSourceUrl() + "\""));
    }

    @Test
    public void testGetMediaByGUID() throws Exception {
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        Media resultMedia = Media.builder()
                .active("true")
                .domain("Lodging")
                .domainId("1234")
                .fileName("47474_freetotbook_5h5h5h5h5h5h.jpg")
                .sourceUrl("http://some-source-url")
                .lcmMediaId("123456")
                .lastUpdated(new Date())
                .lcmMediaId("123456")
                .mediaGuid("d2d4d480-9627-47f9-86c6-1874c18d37f4")
                .build();

        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByGUID(anyString())).thenReturn(transformSingleMediaForResponse(resultMedia));
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        ResponseEntity<String> responseEntity = mediaController.getMedia("d2d4d480-9627-47f9-86c6-1874c18d37f4", mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"domain\":\"Lodging\""));
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\":\"" + resultMedia.getMediaGuid() + "\""));
        assertTrue(responseEntity.getBody().contains("\"domainId\":\"" + resultMedia.getDomainId() + "\""));
        assertTrue(responseEntity.getBody().contains("\"domainFields\":{"));
        assertTrue(responseEntity.getBody().contains("\"lcmMediaId\":\"" + resultMedia.getLcmMediaId() + "\""));
        assertTrue(responseEntity.getBody().contains("\"fileName\":\"" + resultMedia.getFileName() + "\""));
        assertTrue(responseEntity.getBody().contains("\"sourceUrl\":\"" + resultMedia.getSourceUrl() + "\""));
    }

    @Test
    public void testDeleteMediaByGUIDWithLcmIdGUIDFound() throws Exception {
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        MediaDao mockMediaDao = mock(MediaDao.class);
        List<Media> medias = new ArrayList<>();
        Media resultMedia = Media.builder().active("true").domain("Lodging").domainId("1234").fileName("47474_freetotbook_5h5h5h5h5h5h.jpg")
                .lcmMediaId("123456").lastUpdated(new Date()).lcmMediaId("123456").mediaGuid("d2d4d480-9627-47f9-86c6-1874c18d37f4").build();
        medias.add(resultMedia);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(medias);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        ResponseEntity<String> responseEntity = mediaController.deleteMedia("12312", mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaUpdateByGuidHeroWithUnPublishMedia() throws Exception {

        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        List<Media> emptyMediaList = new ArrayList<>();
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(emptyMediaList);
        String dynamoField = "{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"true\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"false\" \n"
                + "         }\n"
                + "      ]\n"
                + "   }";
        Media dynamoMedia =
                Media.builder().fileName("file").domainId("41098").mediaGuid("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6").domainFields(dynamoField)
                        .lcmMediaId("1234").build();
        when(mockMediaDao.getMediaByGuid(anyString())).thenReturn(dynamoMedia);
        final Map<String, String> status = new HashMap<>();
        status.put("file", "PUBLISHED");
        when(mockMediaDao.getLatestStatus(anyObject())).thenReturn(status);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);

        CatalogHeroProcessor catalogHeroProcessor = getCatalogMock();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);

        MultiValueMap<String, String> headers = new HttpHeaders();

        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaUpdateByGuidHeroNull() throws Exception {

        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        List<Media> emptyMediaList = new ArrayList<>();
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(emptyMediaList);
        String dynamoField = "{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"true\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"false\" \n"
                + "         }\n"
                + "      ]\n"
                + "   }";
        Media dynamoMedia =
                Media.builder().fileName("file").lcmMediaId("19671339").domainId("41098").mediaGuid("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6").domainFields(dynamoField)
                        .build();
        when(mockMediaDao.getMediaByGuid(anyString())).thenReturn(dynamoMedia);
        final Map<String, String> status = new HashMap<>();
        status.put("file", "PUBLISHED");
        when(mockMediaDao.getLatestStatus(anyObject())).thenReturn(status);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);

        CatalogHeroProcessor catalogHeroProcessor = getCatalogMock();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);

        MultiValueMap<String, String> headers = new HttpHeaders();

        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaUpdateByGuid() throws Exception {

        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"false\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        List<Media> emptyMediaList = new ArrayList<>();
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(emptyMediaList);
        String dynamoField = "{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"true\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"false\" \n"
                + "         }\n"
                + "      ]\n"
                + "   }";
        Media dynamoMedia =
                Media.builder().fileName("file").lcmMediaId("19671339").domainId("41098").mediaGuid("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6").domainFields(dynamoField)
                        .build();
        when(mockMediaDao.getMediaByGuid(anyString())).thenReturn(dynamoMedia);

        final Map<String, String> status = new HashMap<>();
        status.put("file", "PUBLISHED");
        when(mockMediaDao.getLatestStatus(anyObject())).thenReturn(status);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);

        CatalogHeroProcessor catalogHeroProcessor = getCatalogMock();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);

        MultiValueMap<String, String> headers = new HttpHeaders();

        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaUpdateByGuidWithHeroTrue() throws Exception {

        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        List<Media> emptyMediaList = new ArrayList<>();
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(emptyMediaList);
        String dynamoField = "{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"true\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"false\" \n"
                + "         }\n"
                + "      ]\n"
                + "   }";
        Media dynamoMedia =
                Media.builder().fileName("file").domain("Lodging").lcmMediaId("19671339").domainId("41098").mediaGuid("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6").domainFields(dynamoField)
                        .build();
        when(mockMediaDao.getMediaByGuid(anyString())).thenReturn(dynamoMedia);
        final Map<String, String> status = new HashMap<>();
        status.put("file", "PUBLISHED");
        when(mockMediaDao.getLatestStatus(anyObject())).thenReturn(status);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);

        CatalogHeroProcessor catalogHeroProcessor = getCatalogMockHeroFalse();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);

        MultiValueMap<String, String> headers = new HttpHeaders();

        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaUpdateByMediaId() throws Exception {

        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        List<Media> emptyMediaList = new ArrayList<>();
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(emptyMediaList);

        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);
        MediaUpdateDao mediaUpdateDao = mock(MediaUpdateDao.class);
        LcmMedia lcmMedia = LcmMedia.builder().domainId(41098).build();
        when(mediaUpdateDao.getMediaByMediaId(anyInt())).thenReturn(lcmMedia);
        FieldUtils.writeField(mediaController, "mediaUpdateDao", mediaUpdateDao, true);

        CatalogHeroProcessor catalogHeroProcessor = getCatalogMockHeroFalse();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);

        MultiValueMap<String, String> headers = new HttpHeaders();

        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("19671339", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaUpdateByMediaIdWithHeroFalse() throws Exception {

        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"false\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        List<Media> emptyMediaList = new ArrayList<>();
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(emptyMediaList);

        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);
        MediaUpdateDao mediaUpdateDao = mock(MediaUpdateDao.class);
        LcmMedia lcmMedia = LcmMedia.builder().domainId(41098).build();
        when(mediaUpdateDao.getMediaByMediaId(anyInt())).thenReturn(lcmMedia);
        FieldUtils.writeField(mediaController, "mediaUpdateDao", mediaUpdateDao, true);
        Media dynamoMedia = Media.builder().domain("Lodging").lastUpdated(new Date()).build();
        when(mockMediaDao.getMediaByGUID(anyString())).thenReturn(transformSingleMediaForResponse(dynamoMedia));

        CatalogHeroProcessor catalogHeroProcessor = getCatalogMock();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);

        MultiValueMap<String, String> headers = new HttpHeaders();

        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("19671339", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaUpdateByMediaIdWithOutSubCategoryId() throws Exception {

        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"propertyHero\":\"false\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        List<Media> emptyMediaList = new ArrayList<>();
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(emptyMediaList);

        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);

        MediaUpdateDao mediaUpdateDao = mock(MediaUpdateDao.class);
        LcmMedia lcmMedia = LcmMedia.builder().domainId(41098).build();
        when(mediaUpdateDao.getMediaByMediaId(anyInt())).thenReturn(lcmMedia);
        FieldUtils.writeField(mediaController, "mediaUpdateDao", mediaUpdateDao, true);

        CatalogHeroProcessor catalogHeroProcessor = getCatalogMock();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);

        MultiValueMap<String, String> headers = new HttpHeaders();

        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("19671339", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaUpdateBadRequest() throws Exception {

        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"propertyHero\":\"false\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        List<Media> medias = new ArrayList<>();
        Media media = Media.builder().lcmMediaId("19671339").mediaGuid("test").build();
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        medias.add(media);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(medias);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);
        MultiValueMap<String, String> headers = new HttpHeaders();

        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("19671339", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaUpdateBadRequestWithoutUserId() throws Exception {
        String jsonMsg = "{  \n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"propertyHero\":\"false\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        List<Media> medias = new ArrayList<>();
        Media media = Media.builder().lcmMediaId("19671339").mediaGuid("test").build();
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        medias.add(media);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(medias);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);
        MultiValueMap<String, String> headers = new HttpHeaders();
        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("test", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaUpdateInvalidQueryId() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"propertyHero\":\"false\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdateWithError();
        setFieldValue(mediaController, "mapValidatorList", validators);
        MultiValueMap<String, String> headers = new HttpHeaders();
        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("123a", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaUpdateWithCorrectGuidButMissingLCMMediaId() throws Exception {
        final String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\": \"false\",\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        final String domainFields ="{\"category\":\"21001\",\"lcmMediaId\":\"16343312\"}";
        final Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        final Media resultMedia = Media.builder().active("true").domain("Lodging").domainId("1234").fileName("47474_freetotbook_5h5h5h5h5h5h.jpg")
                .lcmMediaId("null").lastUpdated(new Date()).mediaGuid("12345678-abcd-efab-cdef-123456789012").domainFields(domainFields).build();
        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByGuid(eq("12345678-abcd-efab-cdef-123456789012"))).thenReturn(resultMedia);
        final Map<String, String> status = new HashMap<>();
        status.put("47474_freetotbook_5h5h5h5h5h5h.jpg", "PUBLISHED");
        when(mockMediaDao.getLatestStatus(anyObject())).thenReturn(status);
        CatalogHeroProcessor catalogHeroProcessor = getCatalogMock();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);
        setFieldValue(mediaController, "mapValidatorList", validators);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        MultiValueMap<String, String> headers = new HttpHeaders();
        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("12345678-abcd-efab-cdef-123456789012", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());        
    }
    
    @Test
    public void testMediaUpdateWithCorrectGuidButMissingLCMMediaIdAndNoDomainFields() throws Exception {
        final String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\": \"false\",\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        final Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        final Media resultMedia = Media.builder().active("true").domain("Lodging").domainId("1234").fileName("47474_freetotbook_5h5h5h5h5h5h.jpg")
                .lastUpdated(new Date()).mediaGuid("12345678-abcd-efab-cdef-123456789012").build();
        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByGuid(eq("12345678-abcd-efab-cdef-123456789012"))).thenReturn(resultMedia);
        final Map<String, String> status = new HashMap<>();
        status.put("47474_freetotbook_5h5h5h5h5h5h.jpg", "REJECT");
        when(mockMediaDao.getLatestStatus(anyObject())).thenReturn(status);
        CatalogHeroProcessor catalogHeroProcessor = getCatalogMock();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);

        setFieldValue(mediaController, "mapValidatorList", validators);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        MultiValueMap<String, String> headers = new HttpHeaders();
        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("12345678-abcd-efab-cdef-123456789012", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());        
    }

    @Test
    public void testMediaUpdateWithUncorrectDomainFieldsType() throws Exception {
        final String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\": \"false\",\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        final Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        final Media resultMedia = Media.builder().active("true").domain("Lodging").domainId("1234").fileName("47474_freetotbook_5h5h5h5h5h5h.jpg")
                .lastUpdated(new Date()).mediaGuid("12345678-abcd-efab-cdef-123456789012").domainFields("null").build();
        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByGuid(eq("12345678-abcd-efab-cdef-123456789012"))).thenReturn(resultMedia);
        final Map<String, String> status = new HashMap<>();
        status.put("47474_freetotbook_5h5h5h5h5h5h.jpg", "REJECT");
        when(mockMediaDao.getLatestStatus(anyObject())).thenReturn(status);
        CatalogHeroProcessor catalogHeroProcessor = getCatalogMock();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);
        setFieldValue(mediaController, "mapValidatorList", validators);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        MultiValueMap<String, String> headers = new HttpHeaders();
        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("12345678-abcd-efab-cdef-123456789012", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());        
    }



    @Test
    public void testGetMediaWhichHasAGuidByLcmMediaId() throws Exception {
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        Media resultMedia = Media.builder().active("true").domain("Lodging").domainId("1234").fileName("47474_freetotbook_5h5h5h5h5h5h.jpg")
                .lcmMediaId("123456").lastUpdated(new Date()).lcmMediaId("123456").mediaGuid("d2d4d480-9627-47f9-86c6-1874c18d37f4").build();
        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(Collections.singletonList(resultMedia));
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        ResponseEntity<String> responseEntity = mediaController.getMedia("123456", mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Media GUID d2d4d480-9627-47f9-86c6-1874c18d37f4 exists, please use GUID in request."));
    }

    /**
     * CSPB-532224 Thumbail processor must return 422 error when an unprocessable image is received
     *
     * @throws Exception -
     */
    @Test
    public void testCorruptedImageFailToCreateThumbnail() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", " + "\"generateThumbnail\": \"true\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Legacy\" " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenThrow(new RuntimeException("Unable to generate thumbnail with url: http://i.imgur.com/3PRGFii.jpg"));
        setFieldValue(mediaController, "thumbnailProcessor", thumbnailProcessor);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        LcmDynamoMediaDao mockLcmDynamoMediaDao = mock(LcmDynamoMediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockLcmDynamoMediaDao);
        setFieldValue(mediaController, "dynamoMediaRepository", mock(DynamoMediaRepository.class));

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\""));
        assertTrue(responseEntity.getBody().contains("\"error message\":\"Unable to generate thumbnail with url: http://i.imgur.com/3PRGFii.jpg\""));
        assertTrue(responseEntity.getBody().contains("\"status\":\"REJECTED\""));

        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(1)).log(logEntryCaptor.capture(), eq(reporting));
        verify(queueMessagingTemplateMock, times(0)).send(anyString(), any());
        verifyZeroInteractions(mockLcmDynamoMediaDao);
        verify(thumbnailProcessor, times(1)).createThumbnail(any(ImageMessage.class));
        verifyZeroInteractions(thumbnail);
    }

    @Test
    public void testHiddenPublishedMedia() throws Exception {

        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "   \"hidden\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"false\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        List<Media> emptyMediaList = new ArrayList<>();
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(emptyMediaList);
        String dynamoField = "{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"true\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"false\" \n"
                + "         }\n"
                + "      ]\n"
                + "   }";
        Media dynamoMedia =
                Media.builder().fileName("file").lcmMediaId("19671339").domainId("41098").mediaGuid("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6").domainFields(dynamoField)
                        .build();
        when(mockMediaDao.getMediaByGuid(anyString())).thenReturn(dynamoMedia);

        final Map<String, String> status = new HashMap<>();
        status.put("file", "PUBLISHED");
        when(mockMediaDao.getLatestStatus(anyObject())).thenReturn(status);

        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);

        CatalogHeroProcessor catalogHeroProcessor = getCatalogMock();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);

        MultiValueMap<String, String> headers = new HttpHeaders();

        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Only unpublished media can be hidden"));
    }

    @Test
    public void testHiddenUnPublishedMedia() throws Exception {

        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "   \"hidden\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"false\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        List<Media> emptyMediaList = new ArrayList<>();
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(emptyMediaList);
        String dynamoField = "{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"true\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"false\" \n"
                + "         }\n"
                + "      ]\n"
                + "   }";
        Media dynamoMedia =
                Media.builder().fileName("file").domainId("41098").mediaGuid("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6").domainFields(dynamoField)
                        .build();
        when(mockMediaDao.getMediaByGuid(anyString())).thenReturn(dynamoMedia);
        final Map<String, String> status = new HashMap<>();
        status.put("file", "REJECTED");
        when(mockMediaDao.getLatestStatus(anyObject())).thenReturn(status);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);

        CatalogHeroProcessor catalogHeroProcessor = getCatalogMock();
        MediaUpdateProcessor mockUpdateProcess = getMediaUpdateProcesser(catalogHeroProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mockUpdateProcess);

        MultiValueMap<String, String> headers = new HttpHeaders();

        ResponseEntity<String> responseEntity = mediaController.mediaUpdate("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6", jsonMsg, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void testMediaGetHiddenMedia() throws Exception {
        final String dynamoField = "{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"true\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"false\" \n"
                + "         }\n"
                + "      ]\n"
                + "   }";

        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        Media dynamoMedia =  Media.builder()
                .domainId("41098").mediaGuid("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6")
                .domainFields(dynamoField).hidden(true).build();
        DynamoDBMapper dynamo = mock(DynamoDBMapper.class);
        when(dynamo.load(eq(Media.class),eq("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6"))).thenReturn(dynamoMedia);
        MultiValueMap<String, String> headers = new HttpHeaders();
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        Map<String, List<MapMessageValidator>> validators = getMockValidatorsForUpdate();
        setFieldValue(mediaController, "mapValidatorList", validators);

        ResponseEntity<String> responseEntity = mediaController.getMedia("ab4b02a5-8a2e-4653-bb6a-7b249370bdd6", headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test(expected = RuntimeException.class)
         public void pokeTestAcquireMedia() throws Exception {

        String jsonMessage = "{  \n" + "   \"mediaProviderId\":\"1001\",\n" + "   \"fileUrl\":\"http://www.offtoeurope.com/wp-content/uploads/2010/06/radisson-pool.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n" + "   \"stagingKey\":{  \n" + "      \"externalId\":\"222\",\n" + "      \"providerId\":\"300\",\n"
                + "      \"sourceId\":\"99\"\n" + "   },\n" + "   \"expediaId\":\"123\",\n" + "   \"subcategoryId\":\"801\",\n"
                + "   \"callback\":\"http://multi.source.callback/callback\",\n" + "   \"caption\":\"caption\" \n}";
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);
        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        LcmDynamoMediaDao mockLcmDynamoMediaDao = mock(LcmDynamoMediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockLcmDynamoMediaDao);
        DynamoMediaRepository mockDynamoMediaRepository = mock(DynamoMediaRepository.class);
        setFieldValue(mediaController, "dynamoMediaRepository", mockDynamoMediaRepository);
        RuntimeException exception = new RuntimeException("this is a RuntimeException exception");
        doThrow(exception).when(mockDynamoMediaRepository).storeMediaAddMessage(any(ImageMessage.class), any(null));
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        setFieldValue(mediaController, "hipChatRoom", "EWE CS: Phoenix Notifications");
        Poker poker = mock(Poker.class);
        setFieldValue(mediaController, "poker", poker);
        mockHeader.add("request-id", requestId);

        mediaController.acquireMedia(jsonMessage, mockHeader);
        verify(poker).poke(eq("Media Services failed to process an acquireMedia request - RequestId: " + requestId), eq("EWE CS: Phoenix Notifications"),
                eq(jsonMessage), eq(exception));
    }

    @Test(expected = RuntimeException.class)
    public void pokeTestAddMedia() throws Exception {

        String jsonMessage = "{  \n" + "   \"mediaProviderId\":\"1001\",\n" + "   \"fileUrl\":\"http://www.offtoeurope.com/wp-content/uploads/2010/06/radisson-pool.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n" + "   \"stagingKey\":{  \n" + "      \"externalId\":\"222\",\n" + "      \"providerId\":\"300\",\n"
                + "      \"sourceId\":\"99\"\n" + "   },\n" + "   \"expediaId\":\"123\",\n" + "   \"subcategoryId\":\"801\",\n"
                + "   \"callback\":\"http://multi.source.callback/callback\",\n" + "   \"caption\":\"caption\" \n}";
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);
        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        LcmDynamoMediaDao mockLcmDynamoMediaDao = mock(LcmDynamoMediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockLcmDynamoMediaDao);
        DynamoMediaRepository mockDynamoMediaRepository = mock(DynamoMediaRepository.class);
        setFieldValue(mediaController, "dynamoMediaRepository", mockDynamoMediaRepository);
        RuntimeException exception = new RuntimeException("this is a RuntimeException exception");
        doThrow(exception).when(mockDynamoMediaRepository).storeMediaAddMessage(any(ImageMessage.class), any(null));
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        setFieldValue(mediaController, "hipChatRoom", "EWE CS: Phoenix Notifications");
        Poker poker = mock(Poker.class);
        setFieldValue(mediaController, "poker", poker);
        mockHeader.add("request-id", requestId);

        mediaController.mediaAdd(jsonMessage, mockHeader);
        verify(poker).poke(eq("Media Services failed to process a mediaAdd request - RequestId: " + requestId), eq("EWE CS: Phoenix Notifications"),
                eq(jsonMessage), eq(exception));
    }

    @Test(expected = RuntimeException.class)
    public void pokeTestMediaUpdate() throws Exception {

        String jsonMessage = "{  \n" + "   \"mediaProviderId\":\"1001\",\n" + "   \"fileUrl\":\"http://www.offtoeurope.com/wp-content/uploads/2010/06/radisson-pool.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n" + "   \"stagingKey\":{  \n" + "      \"externalId\":\"222\",\n" + "      \"providerId\":\"300\",\n"
                + "      \"sourceId\":\"99\"\n" + "   },\n" + "   \"expediaId\":\"123\",\n" + "   \"subcategoryId\":\"801\",\n"
                + "   \"callback\":\"http://multi.source.callback/callback\",\n" + "   \"comment\":\"comment\" \n}";
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);
        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        MediaDao mockMediaDao = mock(MediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        RuntimeException exception = new RuntimeException("this is a RuntimeException exception");
        doThrow(exception).when(mockMediaDao).getMediaByMediaId(eq("123456"));
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        setFieldValue(mediaController, "hipChatRoom", "EWE CS: Phoenix Notifications");
        Poker poker = mock(Poker.class);
        setFieldValue(mediaController, "poker", poker);
        mockHeader.add("request-id", requestId);

        mediaController.mediaUpdate("123456", jsonMessage, mockHeader);
        verify(poker).poke(eq("Media Services failed to process a mediaUpdate request - RequestId: " + requestId), eq("EWE CS: Phoenix Notifications"),
                eq(jsonMessage), eq(exception));
    }

    @Test(expected = RuntimeException.class)
    public void pokeTestGetMedia() throws Exception {

        String jsonMessage = "{  \n" + "   \"mediaProviderId\":\"1001\",\n" + "   \"fileUrl\":\"http://www.offtoeurope.com/wp-content/uploads/2010/06/radisson-pool.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n" + "   \"stagingKey\":{  \n" + "      \"externalId\":\"222\",\n" + "      \"providerId\":\"300\",\n"
                + "      \"sourceId\":\"99\"\n" + "   },\n" + "   \"expediaId\":\"123\",\n" + "   \"subcategoryId\":\"801\",\n"
                + "   \"callback\":\"http://multi.source.callback/callback\",\n" + "   \"comment\":\"comment\" \n}";
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);
        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        MediaDao mockMediaDao = mock(MediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        RuntimeException exception = new RuntimeException("this is a RuntimeException exception");
        doThrow(exception).when(mockMediaDao).getMediaByGUID(eq("156b1953-3bb4-404f-8773-edc04b3d8562"));
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        setFieldValue(mediaController, "hipChatRoom", "EWE CS: Phoenix Notifications");
        Poker poker = mock(Poker.class);
        setFieldValue(mediaController, "poker", poker);
        mockHeader.add("request-id", requestId);

        mediaController.getMedia(eq("156b1953-3bb4-404f-8773-edc04b3d8562"), mockHeader);
        verify(poker).poke(eq("Media Services failed to process a getMedia request - RequestId: " + requestId), eq("EWE CS: Phoenix Notifications"),
                eq(jsonMessage), eq(exception));
    }

    @Test(expected = RuntimeException.class)
    public void pokeTestGetMediaByDomainId() throws Exception {

        String jsonMessage = "{  \n" + "   \"mediaProviderId\":\"1001\",\n" + "   \"fileUrl\":\"http://www.offtoeurope.com/wp-content/uploads/2010/06/radisson-pool.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n" + "   \"stagingKey\":{  \n" + "      \"externalId\":\"222\",\n" + "      \"providerId\":\"300\",\n"
                + "      \"sourceId\":\"99\"\n" + "   },\n" + "   \"expediaId\":\"123\",\n" + "   \"subcategoryId\":\"801\",\n"
                + "   \"callback\":\"http://multi.source.callback/callback\",\n" + "   \"comment\":\"comment\" \n}";
        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);
        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        MediaDao mockMediaDao = mock(MediaDao.class);
        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        RuntimeException exception = new RuntimeException("this is a RuntimeException exception");
        doThrow(exception).when(mockMediaDao).getMediaByGUID(eq("156b1953-3bb4-404f-8773-edc04b3d8562"));
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        setFieldValue(mediaController, "hipChatRoom", "EWE CS: Phoenix Notifications");
        Poker poker = mock(Poker.class);
        setFieldValue(mediaController, "poker", poker);
        mockHeader.add("request-id", requestId);

        mediaController.getMediaByDomainId("lodging", "123", 100, 8, "true", "", "", mockHeader);
        verify(poker).poke(eq("Media Services failed to process a getMedia request - RequestId: " + requestId), eq("EWE CS: Phoenix Notifications"),
                eq(jsonMessage), eq(exception));
    }

    @Test
    public void testInvalidPagination() throws Exception {
        mediaController = new MediaController();
        MultiValueMap<String, String> headers = new HttpHeaders();
        SKUGroupCatalogItemDao skuGroupCatalogItemDao = mock(SKUGroupCatalogItemDao.class);
        when(skuGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(true);
        LcmDynamoMediaDao lcmDynamoMediaDao = new LcmDynamoMediaDao();
        setFieldValue(mediaController, "mediaDao", lcmDynamoMediaDao);
        DynamoMediaRepository mockMediaRepo = mock(DynamoMediaRepository.class);
        setFieldValue(lcmDynamoMediaDao, "mediaRepo", mockMediaRepo);
        when(mockMediaRepo.loadMedia(Domain.LODGING, "1234")).thenReturn(new ArrayList<>());
        SQLMediaListSproc mockLcmMediaListSproc = mock(SQLMediaListSproc.class);
        setFieldValue(lcmDynamoMediaDao, "lcmMediaListSproc", mockLcmMediaListSproc);
        Map mockMap = mock(Map.class);
        when(mockLcmMediaListSproc.execute(1234)).thenReturn(mockMap);
        when(mockMap.get(SQLMediaListSproc.MEDIA_SET)).thenReturn(new ArrayList<LcmMediaAndDerivative>());
        SQLRoomGetByCatalogItemIdSproc mockRoomGetByCatalogItemIdSproc = mock(SQLRoomGetByCatalogItemIdSproc.class);
        when(mockRoomGetByCatalogItemIdSproc.execute(1234)).thenReturn(mockMap);
        when(mockMap.get(SQLRoomGetSproc.ROOM_SET)).thenReturn(new ArrayList<LcmMediaRoom>());
        setFieldValue(lcmDynamoMediaDao, "roomGetByCatalogItemIdSproc", mockRoomGetByCatalogItemIdSproc);

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);
        setFieldValue(mediaController, "skuGroupCatalogItemDao", skuGroupCatalogItemDao);
        ResponseEntity<String> responseEntity = mediaController.getMediaByDomainId("Lodging", "1234", 10, 500, "true", null, null, headers);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("pageIndex is out of bounds"));
    }
    
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testReplaceAOldMedia() throws Exception {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(MEDIA_CLOUD_ROUTER_CLIENT_ID);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"123_1_NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", " + "\"domain\": \"Lodging\", " + "\"domainId\": \"123\", " + "\"domainProvider\": \"SCORE\", "
                + "\"domainFields\": { " + "    \"replace\": \"true\" " + "  } " + "}";

        Map<String, List<MapMessageValidator>> validators = getMockValidators();
        setFieldValue(mediaController, "mapValidatorList", validators);

        LogActivityProcess mockLogActivityProcess = mock(LogActivityProcess.class);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);

        MediaDao mockMediaDao = mock(MediaDao.class);
        when(mockMediaDao.getMediaByFilename(eq("123_1_NASA_ISS-4.jpg"))).thenReturn(new ArrayList<>());
        final LcmMedia media1 = LcmMedia.builder().active(true).domainId(123).fileName("123_1_NASA_ISS-4.jpg").mediaId(12345).build();
        when(mockMediaDao.getMediaByFilenameInLCM(eq(123), eq("123_1_NASA_ISS-4.jpg")))
        .thenReturn(Lists.newArrayList(media1));

        setFieldValue(mediaController, "mediaDao", mockMediaDao);
        setFieldValue(mediaController, "dynamoMediaRepository", dynamoMediaRepository);
        String requestId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = mediaController.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(mockLogActivityProcess, times(2)).log(logEntryCaptor.capture(), eq(reporting));
        ArgumentCaptor<Message> publishedMessage = ArgumentCaptor.forClass(Message.class);
        verify(queueMessagingTemplateMock, times(1)).send(anyString(), publishedMessage.capture());
        final Message<String> publishedMessageValue = publishedMessage.getValue();
        assertTrue(publishedMessageValue.getPayload().contains("\"fileName\":\"123_1_NASA_ISS-4.jpg\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"active\":\"true\""));
        assertTrue(publishedMessageValue.getPayload().contains("\"clientId\":\"" + MEDIA_CLOUD_ROUTER_CLIENT_ID));
        assertTrue(publishedMessageValue.getPayload().contains("\"requestId\":\"" + requestId));
        assertTrue(publishedMessageValue.getPayload().contains("\"lcmMediaId\":\"" + "12345"));
        verify(mockMediaDao, times(1)).getMediaByFilename(eq("123_1_NASA_ISS-4.jpg"));
        verify(mockMediaDao, times(1)).getMediaByFilenameInLCM(eq(123), eq("123_1_NASA_ISS-4.jpg"));
        verify(dynamoMediaRepository, times(1)).storeMediaAddMessage(any(ImageMessage.class), any(Thumbnail.class));
    }



    @SuppressWarnings({"unchecked"})
    private static Map<String, List<MapMessageValidator>> getMockValidators() {
        Map<String, List<MapMessageValidator>> validators = new HashMap<>();
        List<MapMessageValidator> messageValidator = new ArrayList<>();
        MapMessageValidator mockMessageValidator = mock(MapMessageValidator.class);
        List<String> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(anyList())).thenReturn(validationErrorList);
        messageValidator.add(mockMessageValidator);
        validators.put(TEST_CLIENT_ID, messageValidator);
        validators.put(MEDIA_CLOUD_ROUTER_CLIENT_ID, messageValidator);
        validators.put(MULTISOURCE, messageValidator);
        return validators;
    }

    @SuppressWarnings({"unchecked"})
    private static Map<String, List<MapMessageValidator>> getMockValidatorsForUpdate() {
        Map<String, List<MapMessageValidator>> validators = new HashMap<>();
        List<MapMessageValidator> messageValidator = new ArrayList<>();
        MapMessageValidator mockMessageValidator = mock(MapMessageValidator.class);
        List<String> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(anyList())).thenReturn(validationErrorList);
        messageValidator.add(mockMessageValidator);
        validators.put("EPCUpdate", messageValidator);
        return validators;
    }

    @SuppressWarnings({"unchecked"})
    private static Map<String, List<MapMessageValidator>> getMockValidatorsForUpdateWithError() {
        Map<String, List<MapMessageValidator>> validators = new HashMap<>();
        List<MapMessageValidator> messageValidator = new ArrayList<>();
        MapMessageValidator mockMessageValidator = mock(MapMessageValidator.class);
        List<String> validationErrorList = new ArrayList<>();
        validationErrorList.add("test");
        when(mockMessageValidator.validateImages(anyList())).thenReturn(validationErrorList);
        messageValidator.add(mockMessageValidator);
        validators.put("EPCUpdate", messageValidator);
        return validators;
    }

    private MediaUpdateProcessor getMediaUpdateProcesser(CatalogHeroProcessor catalogHeroProcessor) throws Exception {
        MediaUpdateProcessor mockUpdateProcess = new MediaUpdateProcessor();
        CatalogItemMediaDao catalogItemMediaDao = mock(CatalogItemMediaDao.class);
        MediaDao mediaDao = mock(MediaDao.class);
        MediaUpdateDao mediaUpdateDao = mock(MediaUpdateDao.class);
        LcmMedia lcmMedia = LcmMedia.builder().domainId(41098).build();
        when(mediaUpdateDao.getMediaByMediaId(anyInt())).thenReturn(lcmMedia);
        Mockito.doNothing().when(mediaUpdateDao).updateMedia(any(), anyInt());
        FieldUtils.writeField(mockUpdateProcess, "mediaUpdateDao", mediaUpdateDao, true);

        Mockito.doNothing().when(mediaDao).saveMedia(any());
        FieldUtils.writeField(mockUpdateProcess, "mediaDao", mediaDao, true);

        LcmMediaRoom lcmMediaRoom = LcmMediaRoom.builder().roomId(928675).roomHero(true).build();
        LcmMediaRoom lcmMediaRoom2 = LcmMediaRoom.builder().roomId(934779).roomHero(true).build();
        LcmMediaRoom lcmMediaRoom3 = LcmMediaRoom.builder().roomId(928678).roomHero(true).build();

        List<LcmMediaRoom> lcmMediaRoomList = new ArrayList<>();
        lcmMediaRoomList.add(lcmMediaRoom);
        lcmMediaRoomList.add(lcmMediaRoom2);
        lcmMediaRoomList.add(lcmMediaRoom3);
        when(catalogItemMediaDao.getLcmRoomsByMediaId(anyInt())).thenReturn(lcmMediaRoomList);
        Mockito.doNothing().when(catalogItemMediaDao).deleteParagraph(anyInt());
        Mockito.doNothing().when(catalogItemMediaDao).deleteCatalogItem(anyInt(), anyInt());
        Mockito.doNothing().when(catalogItemMediaDao).addOrUpdateParagraph(anyInt(), anyInt());
        Mockito.doNothing().when(catalogItemMediaDao).addCatalogItemForRoom(anyInt(), anyInt(), anyInt(), anyObject());
        FieldUtils.writeField(mockUpdateProcess, "catalogItemMediaDao", catalogItemMediaDao, true);
        FieldUtils.writeField(mockUpdateProcess, "catalogHeroProcessor", catalogHeroProcessor, true);
        setFieldValue(mockUpdateProcess, "kafkaPublisher", kafkaPublisher);
        return mockUpdateProcess;
    }

    private CatalogHeroProcessor getCatalogMock() throws Exception {
        CatalogHeroProcessor catalogHeroProcessor = new CatalogHeroProcessor();
        CatalogItemMediaDao catalogItemMediaDao = mock(CatalogItemMediaDao.class);
        CatalogItemMediaChgSproc catalogItemMediaChgSproc = mock(CatalogItemMediaChgSproc.class);
        MediaDao mediaDao = mock(MediaDao.class);

        List<LcmCatalogItemMedia> lcmCatalogItemMediaList = new ArrayList<>();
        LcmCatalogItemMedia lcmCatalogItemMedia =
                LcmCatalogItemMedia.builder().catalogItemId(41098).mediaId(19671339).mediaUseRank(3).lastUpdatedBy("test").lastUpdateDate(new Date())
                        .build();
        lcmCatalogItemMediaList.add(lcmCatalogItemMedia);

        FieldUtils.writeField(catalogHeroProcessor, "catalogItemMediaChgSproc", catalogItemMediaChgSproc, true);
        Mockito.doNothing().when(catalogItemMediaDao).updateCatalogItem(any(), anyInt(), anyInt());
        FieldUtils.writeField(catalogHeroProcessor, "catalogItemMediaDao", catalogItemMediaDao, true);
        FieldUtils.writeField(catalogHeroProcessor, "mediaDao", mediaDao, true);

        List<Media> heroMedia = new ArrayList<>();
        DynamoMediaRepository mediaRepo = mock(DynamoMediaRepository.class);
        String dynamoField = "{  \n"
                + "      \"subcategoryId\":\"2201\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"true\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"false\" \n"
                + "         }\n"
                + "      ]\n"
                + "   }";
        Media media =
                Media.builder().lcmMediaId("196713").mediaGuid("testGuid").domainId("41098").domainFields(dynamoField).lastUpdated(
                        new Date(new Date().getTime() - 10000)).build();
        heroMedia.add(media);
        when(mediaRepo.retrieveHeroPropertyMedia(anyString(), anyString())).thenReturn(heroMedia);
        FieldUtils.writeField(catalogHeroProcessor, "mediaRepo", mediaRepo, true);

        CatalogItemMediaGetSproc catalogItemMediaGetSproc = mock(CatalogItemMediaGetSproc.class);
        when(catalogItemMediaGetSproc.getMedia(anyInt(), anyInt())).thenReturn(lcmCatalogItemMediaList);
        FieldUtils.writeField(catalogHeroProcessor, "catalogItemMediaGetSproc", catalogItemMediaGetSproc, true);

        MediaLstWithCatalogItemMediaAndMediaFileNameSproc mediaLstWithCatalogItemMediaAndMediaFileNameSproc =
                mock(MediaLstWithCatalogItemMediaAndMediaFileNameSproc.class);
        when(mediaLstWithCatalogItemMediaAndMediaFileNameSproc.getMedia(anyInt())).thenReturn(lcmCatalogItemMediaList);
        FieldUtils.writeField(catalogHeroProcessor, "mediaLstWithCatalogItemMediaAndMediaFileNameSproc", mediaLstWithCatalogItemMediaAndMediaFileNameSproc,
                true);

        return catalogHeroProcessor;
    }

    private CatalogHeroProcessor getCatalogMockHeroFalse() throws Exception {
        CatalogHeroProcessor catalogHeroProcessor = new CatalogHeroProcessor();
        CatalogItemMediaDao catalogItemMediaDao = mock(CatalogItemMediaDao.class);
        CatalogItemMediaChgSproc catalogItemMediaChgSproc = mock(CatalogItemMediaChgSproc.class);
        MediaDao mediaDao = mock(MediaDao.class);

        List<LcmCatalogItemMedia> lcmCatalogItemMediaList = new ArrayList<>();
        LcmCatalogItemMedia lcmCatalogItemMedia =
                LcmCatalogItemMedia.builder().catalogItemId(41098).mediaId(19671339).mediaUseRank(0).lastUpdatedBy("test").lastUpdateDate(new Date())
                        .build();
        LcmCatalogItemMedia lcmCatalogItemMedia2 =
                LcmCatalogItemMedia.builder().catalogItemId(41098).mediaId(19671337).mediaUseRank(0).lastUpdatedBy("test").lastUpdateDate(new Date())
                        .build();
        lcmCatalogItemMediaList.add(lcmCatalogItemMedia);
        lcmCatalogItemMediaList.add(lcmCatalogItemMedia2);

        FieldUtils.writeField(catalogHeroProcessor, "catalogItemMediaChgSproc", catalogItemMediaChgSproc, true);
        Mockito.doNothing().when(catalogItemMediaDao).updateCatalogItem(any(), anyInt(), anyInt());
        FieldUtils.writeField(catalogHeroProcessor, "catalogItemMediaDao", catalogItemMediaDao, true);
        FieldUtils.writeField(catalogHeroProcessor, "mediaDao", mediaDao, true);

        String dynamoField = "{  \n"
                + "      \"subcategoryId\":\"2201\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"true\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"false\" \n"
                + "         }\n"
                + "      ]\n"
                + "   }";
        List<Media> heroMedia = new ArrayList<>();
        DynamoMediaRepository mediaRepo = mock(DynamoMediaRepository.class);
        Media media =
                Media.builder().lcmMediaId("19671338").mediaGuid("testGuid").domainId("41098").domainFields(dynamoField).lastUpdated(
                        new Date(new Date().getTime() - 10000)).build();
        heroMedia.add(media);
        when(mediaRepo.retrieveHeroPropertyMedia(anyString(), anyString())).thenReturn(heroMedia);
        FieldUtils.writeField(catalogHeroProcessor, "mediaRepo", mediaRepo, true);

        CatalogItemMediaGetSproc catalogItemMediaGetSproc = mock(CatalogItemMediaGetSproc.class);
        when(catalogItemMediaGetSproc.getMedia(anyInt(), anyInt())).thenReturn(lcmCatalogItemMediaList);
        FieldUtils.writeField(catalogHeroProcessor, "catalogItemMediaGetSproc", catalogItemMediaGetSproc, true);

        MediaLstWithCatalogItemMediaAndMediaFileNameSproc mediaLstWithCatalogItemMediaAndMediaFileNameSproc =
                mock(MediaLstWithCatalogItemMediaAndMediaFileNameSproc.class);
        when(mediaLstWithCatalogItemMediaAndMediaFileNameSproc.getMedia(anyInt())).thenReturn(lcmCatalogItemMediaList);
        FieldUtils.writeField(catalogHeroProcessor, "mediaLstWithCatalogItemMediaAndMediaFileNameSproc", mediaLstWithCatalogItemMediaAndMediaFileNameSproc,
                true);
        //when(catalogHeroProcessor.getCatalogItemMedia(anyInt(), anyInt())).thenReturn(lcmCatalogItemMedia);

        return catalogHeroProcessor;
    }

    private MediaGetResponse transformSingleMediaForResponse(Media media) {
        /* @formatter:off */
        setResponseLcmMediaId(media);
        return MediaGetResponse.builder()
                .mediaGuid(media.getMediaGuid())
                .fileUrl(media.getFileUrl())
                .sourceUrl(media.getSourceUrl())
                .fileName(media.getFileName())
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

    private List<DomainIdMedia> transformMediaListForResponse(List<Media> mediaList) {
        return mediaList.stream().map(media -> {
            setResponseLcmMediaId(media);
            /* @formatter:off */
            return DomainIdMedia.builder()
                    .mediaGuid(media.getMediaGuid())
                    .fileUrl(media.getFileUrl())
                    .sourceUrl(media.getSourceUrl())
                    .fileName(media.getFileName())
                    .active(media.getActive())
                    .width(media.getWidth())
                    .height(media.getHeight())
                    .fileSize(media.getFileSize())
                    .status(media.getStatus())
                    .lastUpdatedBy(media.getUserId())
                    .lastUpdateDateTime(DATE_FORMATTER.print(media.getLastUpdated().getTime()))
                    .domainProvider(media.getProvider())
                    .domainFields(media.getDomainData())
                    .derivatives(media.getDerivativesList())
                    .domainDerivativeCategory(media.getDomainDerivativeCategory())
                    .comments((media.getCommentList() == null) ? null: media.getCommentList().stream()
                            .map(comment -> Comment.builder().note(comment)
                                    .timestamp(DATE_FORMATTER.print(media.getLastUpdated().getTime())).build())
                            .collect(Collectors.toList()))
                    .build();
        }).collect(Collectors.toList());
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
}
