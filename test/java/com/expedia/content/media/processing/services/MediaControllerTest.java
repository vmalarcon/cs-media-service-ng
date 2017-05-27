package com.expedia.content.media.processing.services;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.kafka.KafkaCommonPublisher;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.reqres.DomainIdMedia;
import com.expedia.content.media.processing.services.reqres.MediaByDomainIdResponse;
import com.expedia.content.media.processing.services.reqres.MediaGetResponse;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.validator.MapMessageValidator;

@RunWith(MockitoJUnitRunner.class)
public class MediaControllerTest {

    private static final String TEST_CLIENT_ID = "a-user";

    @Mock
    private Reporting reporting;
    @Mock
    private LogActivityProcess mockLogActivityProcess;
    @Mock
    private QueueMessagingTemplate queueMessagingTemplateMock;
    @Mock
    private KafkaCommonPublisher kafkaCommonPublisher;
    @Mock
    private MediaDao mediaDBMediaDao;
    @Mock
    private MediaAddProcessor mediaAddProcessor;
    @Mock
    private MediaUpdateProcessor mediaUpdateProcessor;
    @Mock
    private Map<String, List<MapMessageValidator>> mockValidators;
    @Mock
    private MapMessageValidator mockMessageValidator;

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
    public void initialize() throws IllegalAccessException, NoSuchFieldException {
        mediaController = new MediaController();
        setFieldValue(mediaController, "kafkaCommonPublisher", kafkaCommonPublisher);
        setFieldValue(mediaController, "mediaDBMediaDao", mediaDBMediaDao);
        setFieldValue(mediaController, "mediaAddProcessor", mediaAddProcessor);
        setFieldValue(mediaController, "mediaUpdateProcessor", mediaUpdateProcessor);
        setFieldValue(mediaController, "mapValidatorList", mockValidators);
        setFieldValue(mediaController, "logActivityProcess", mockLogActivityProcess);
        setFieldValue(mediaController, "messagingTemplate", queueMessagingTemplateMock);
        setFieldValue(mediaController, "reporting", reporting);
        mediaControllerSpy = spy(mediaController);
        Mockito.doNothing().when(kafkaCommonPublisher).publishImageMessage(anyObject(),anyString());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testValidateImageAddSuccess() throws Exception {
        String jsonMessage = "{ "
                + "\"fileUrl\": \"https://i.imgur.com/3PRGFii.jpg\", "
                + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", "
                + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\" "
                + "}";

        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(true, "yup", HttpStatus.ACCEPTED.toString())).when(mediaControllerSpy).verifyUrl(anyString());
        List<MapMessageValidator> validatorList = new ArrayList<>();
        List<String> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(any())).thenReturn(validationErrorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.get(anyString())).thenReturn(validatorList);
        when(mockValidators.getOrDefault(eq("a-user"), any())).thenReturn(validatorList);
        when(mediaAddProcessor.processRequest(eq(jsonMessage), eq(requestId), anyString(), anyString(), eq(HttpStatus.ACCEPTED), any(Date.class)))
                .thenReturn(new ResponseEntity<String>("You did it!", HttpStatus.ACCEPTED));
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.ACCEPTED, responseEntity.getStatusCode());
        verify(mediaAddProcessor, times(1)).processRequest(anyString(), anyString(), anyString(), anyString(), any(HttpStatus.class), any(Date.class));
        verifyZeroInteractions(mediaUpdateProcessor);
    }

    @Test
    public void testValidateNonValidImageMessage() throws Exception {
        String jsonMessage = "{ "
                + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", "
                + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\" "
                + "}";

        String requestId = "12345678-1234-1234";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(true, "yup", HttpStatus.ACCEPTED.toString())).when(mediaControllerSpy).verifyUrl(anyString());
        List<MapMessageValidator> validatorList = new ArrayList<>();
        List<String> validationErrorList = new ArrayList<>();
        validationErrorList.add("test Error");
        when(mockMessageValidator.validateImages(any())).thenReturn(validationErrorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.get(anyString())).thenReturn(validatorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.getOrDefault(eq("a-user"), any())).thenReturn(validatorList);
        when(mediaAddProcessor.processRequest(eq(jsonMessage), eq(requestId), anyString(), anyString(), eq(HttpStatus.ACCEPTED), any(Date.class)))
                .thenReturn(new ResponseEntity<String>("You did it!", HttpStatus.ACCEPTED));
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("test Error"));
        verifyZeroInteractions(mediaAddProcessor);
        verifyZeroInteractions(mediaUpdateProcessor);
    }

    @Test
    public void testValidateUnauthorizedUserImageMessage() throws Exception {
        String jsonMessage = "{ "
                + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", "
                + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\" "
                + "}";

        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(true, "yup", HttpStatus.ACCEPTED.toString())).when(mediaControllerSpy).verifyUrl(anyString());
        List<MapMessageValidator> validatorList = null;
        List<String> validationErrorList = null;
        when(mockMessageValidator.validateImages(any())).thenReturn(validationErrorList);
        when(mockValidators.get(anyString())).thenReturn(validatorList);
        when(mockValidators.getOrDefault(eq("a-user"), any())).thenReturn(validatorList);
        when(mediaAddProcessor.processRequest(eq(jsonMessage), eq(requestId), anyString(), anyString(), eq(HttpStatus.ACCEPTED), any(Date.class)))
                .thenReturn(new ResponseEntity<String>("You did it!", HttpStatus.ACCEPTED));
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("User is not authorized."));
        verifyZeroInteractions(mediaAddProcessor);
        verifyZeroInteractions(mediaUpdateProcessor);
    }

    @Test
    public void testValidateImageAddNotFound() throws Exception {
        String jsonMessage = "{ "
                + "\"fileUrl\": \"https://i.imgur.com/3PRGFii.jpg\", "
                + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", "
                + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\" "
                + "}";

        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(false, "not found", "not found")).when(mediaControllerSpy).verifyUrl(anyString());
        List<MapMessageValidator> validatorList = new ArrayList<>();
        List<String> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(any())).thenReturn(validationErrorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.get(anyString())).thenReturn(validatorList);
        when(mockValidators.getOrDefault(eq("a-user"), any())).thenReturn(validatorList);
         ResponseEntity<String> responseEntity = mediaControllerSpy.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("not found"));
        verifyZeroInteractions(mediaAddProcessor);
        verifyZeroInteractions(mediaUpdateProcessor);
    }

    @Test
    public void testValidateImageAddZeroBytes() throws Exception {
        String jsonMessage = "{ "
                + "\"fileUrl\": \"https://i.imgur.com/3PRGFii.jpg\", "
                + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", "
                + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\" "
                + "}";

        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(false, "0 Bytes", "0 Bytes")).when(mediaControllerSpy).verifyUrl(anyString());
        List<MapMessageValidator> validatorList = new ArrayList<>();
        List<String> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(any())).thenReturn(validationErrorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.get(anyString())).thenReturn(validatorList);
        when(mockValidators.getOrDefault(eq("a-user"), any())).thenReturn(validatorList);
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("0 Bytes"));
        verifyZeroInteractions(mediaAddProcessor);
        verifyZeroInteractions(mediaUpdateProcessor);
    }

    @Test
    public void testValidateImageAddBadRequest() throws Exception {
        String jsonMessage = "{ "
                + "\"fileUrl\": \"https://i.imgur.com/3PRGFii.jpg\", "
                + "\"fileName\": \"NASA_ISS-4.jpg\", "
                + "\"userId\": \"bobthegreat\", "
                + "\"domain\": \"Lodging\", "
                + "\"domainId\": \"1238\", "
                + "\"domainProvider\": \"EPC Internal User\" "
                + "}";

        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(false, "Something Different", "status")).when(mediaControllerSpy).verifyUrl(anyString());
        List<MapMessageValidator> validatorList = new ArrayList<>();
        List<String> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(any())).thenReturn(validationErrorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.get(anyString())).thenReturn(validatorList);
        when(mockValidators.getOrDefault(eq("a-user"), any())).thenReturn(validatorList);
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaAdd(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Something Different"));
        verifyZeroInteractions(mediaAddProcessor);
        verifyZeroInteractions(mediaUpdateProcessor);
    }

    @Test
    public void testValidateMediaUpdateSuccess() throws Exception {
        String jsonMessage = "{ "
                + "\"userId\": \"bobthegreat\", "
                + "\"domainFields\": {"
                + "     \"subcategoryId\": \"20001\""
                + "}, "
                + "\"comment\": \"I am the greatest image ever\""
                + "}";
        String mediaGuid = "87654321-4321-4321-4321-605040302010";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(true, "yup", HttpStatus.ACCEPTED.toString())).when(mediaControllerSpy).verifyUrl(anyString());
        Media media = Media.builder().mediaGuid(mediaGuid).lcmMediaId("12345").domain("Lodging").active("true").domainId("54321").build();
        when(mediaDBMediaDao.getMediaByGuid(eq(mediaGuid))).thenReturn(media);
        List<MapMessageValidator> validatorList = new ArrayList<>();
        List<String> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(any())).thenReturn(validationErrorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.get(anyString())).thenReturn(validatorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.getOrDefault(eq("EPCUpdate"), any())).thenReturn(validatorList);
        when(mediaUpdateProcessor.processRequest(any(ImageMessage.class), eq(media))).thenReturn(new ResponseEntity<String>("You did it!", HttpStatus.OK));
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaUpdate(mediaGuid, jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("You did it!"));
        verify(mediaUpdateProcessor, times(1)).processRequest(any(ImageMessage.class), eq(media));
        verifyZeroInteractions(mediaAddProcessor);
    }

    @Test
    public void testValidateMediaUpdateSuccessWithHidden() throws Exception {
        String jsonMessage = "{ "
                + "\"userId\": \"bobthegreat\", "
                + "\"domainFields\": {"
                + "     \"subcategoryId\": \"20001\""
                + "}, "
                + "\"hidden\": \"true\", "
                + "\"comment\": \"I am the greatest image ever\""
                + "}";
        String mediaGuid = "87654321-4321-4321-4321-605040302010";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(true, "yup", HttpStatus.ACCEPTED.toString())).when(mediaControllerSpy).verifyUrl(anyString());
        Media media = Media.builder().mediaGuid(mediaGuid).lcmMediaId("12345").status("REJECTED").domain("Lodging").active("true").domainId("54321").build();
        when(mediaDBMediaDao.getMediaByGuid(eq(mediaGuid))).thenReturn(media);
        List<MapMessageValidator> validatorList = new ArrayList<>();
        List<String> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(any())).thenReturn(validationErrorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.get(anyString())).thenReturn(validatorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.getOrDefault(eq("EPCUpdate"), any())).thenReturn(validatorList);
        when(mediaUpdateProcessor.processRequest(any(ImageMessage.class), eq(media))).thenReturn(new ResponseEntity<String>("You did it!", HttpStatus.OK));
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaUpdate(mediaGuid, jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("You did it!"));
        verify(mediaUpdateProcessor, times(1)).processRequest(any(ImageMessage.class), eq(media));
        verifyZeroInteractions(mediaAddProcessor);
    }

    @Test
    public void testValidateMediaUpdateCannotBeHidden() throws Exception {
        String jsonMessage = "{ "
                + "\"userId\": \"bobthegreat\", "
                + "\"domainFields\": {"
                + "     \"subcategoryId\": \"20001\""
                + "}, "
                + "\"hidden\": \"true\", "
                + "\"comment\": \"I am the greatest image ever\""
                + "}";
        String mediaGuid = "87654321-4321-4321-4321-605040302010";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(true, "yup", HttpStatus.ACCEPTED.toString())).when(mediaControllerSpy).verifyUrl(anyString());
        Media media = Media.builder().mediaGuid(mediaGuid).lcmMediaId("12345").status("PUBLISHED").domain("Lodging").active("true").domainId("54321").build();
        when(mediaDBMediaDao.getMediaByGuid(eq(mediaGuid))).thenReturn(media);
        List<MapMessageValidator> validatorList = new ArrayList<>();
        List<String> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(any())).thenReturn(validationErrorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.get(anyString())).thenReturn(validatorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.getOrDefault(eq("EPCUpdate"), any())).thenReturn(validatorList);
        when(mediaUpdateProcessor.processRequest(any(ImageMessage.class), eq(media))).thenReturn(new ResponseEntity<String>("You did it!", HttpStatus.OK));
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaUpdate(mediaGuid, jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Only unpublished media can be hidden"));
        verifyZeroInteractions(mediaUpdateProcessor);
        verifyZeroInteractions(mediaAddProcessor);
    }

    // TODO: Remove this test when services are domain agnostic
    @Test
    public void testValidateMediaUpdateNonLodgingDomain() throws Exception {
        String jsonMessage = "{ "
                + "\"userId\": \"bobthegreat\", "
                + "\"domainFields\": {"
                + "     \"subcategoryId\": \"20001\""
                + "}, "
                + "\"comment\": \"I am the greatest image ever\""
                + "}";
        String mediaGuid = "87654321-4321-4321-4321-605040302010";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(true, "yup", HttpStatus.ACCEPTED.toString())).when(mediaControllerSpy).verifyUrl(anyString());
        Media media = Media.builder().mediaGuid(mediaGuid).lcmMediaId("12345").status("PUBLISHED").domain("Cars").active("true").domainId("54321").build();
        when(mediaDBMediaDao.getMediaByGuid(eq(mediaGuid))).thenReturn(media);
        List<MapMessageValidator> validatorList = new ArrayList<>();
        List<String> validationErrorList = new ArrayList<>();
        when(mockMessageValidator.validateImages(any())).thenReturn(validationErrorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.get(anyString())).thenReturn(validatorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.getOrDefault(eq("EPCUpdate"), any())).thenReturn(validatorList);
        when(mediaUpdateProcessor.processRequest(any(ImageMessage.class), eq(media))).thenReturn(new ResponseEntity<String>("You did it!", HttpStatus.OK));
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaUpdate(mediaGuid, jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Only Lodging Media Updates can be handled at this time"));
        verifyZeroInteractions(mediaUpdateProcessor);
        verifyZeroInteractions(mediaAddProcessor);
    }

    @Test
    public void testValidateMediaUpdateNotAValidRequest() throws Exception {
        String jsonMessage = "{ "
                + "\"userId\": \"bobthegreat\", "
                + "\"domainFields\": {"
                + "     \"subcategoryId\": \"20001\""
                + "}, "
                + "\"comment\": \"I am the greatest image ever\""
                + "}";
        String mediaGuid = "87654321-4321-4321-4321-605040302010";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(true, "yup", HttpStatus.ACCEPTED.toString())).when(mediaControllerSpy).verifyUrl(anyString());
        Media media = Media.builder().mediaGuid(mediaGuid).lcmMediaId("12345").status("PUBLISHED").domain("Lodging").active("true").domainId("54321").build();
        when(mediaDBMediaDao.getMediaByGuid(eq(mediaGuid))).thenReturn(media);
        List<MapMessageValidator> validatorList = new ArrayList<>();
        List<String> validationErrorList = new ArrayList<>();
        validationErrorList.add("an error occurred!");
        when(mockMessageValidator.validateImages(any())).thenReturn(validationErrorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.get(anyString())).thenReturn(validatorList);
        validatorList.add(mockMessageValidator);
        when(mockValidators.getOrDefault(eq("EPCUpdate"), any())).thenReturn(validatorList);
        when(mediaUpdateProcessor.processRequest(any(ImageMessage.class), eq(media))).thenReturn(new ResponseEntity<String>("You did it!", HttpStatus.OK));
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaUpdate(mediaGuid, jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("an error occurred!"));
        verifyZeroInteractions(mediaUpdateProcessor);
        verifyZeroInteractions(mediaAddProcessor);
    }

    @Test
    public void testValidateMediaUpdateNotAValidGuid() throws Exception {
        String jsonMessage = "{ "
                + "\"userId\": \"bobthegreat\", "
                + "\"domainFields\": {"
                + "     \"subcategoryId\": \"20001\""
                + "}, "
                + "\"comment\": \"I am the greatest image ever\""
                + "}";
        String mediaGuid = "not-a-valid-guid";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaUpdate(mediaGuid, jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("input queryId is invalid"));
        verifyZeroInteractions(mediaUpdateProcessor);
        verifyZeroInteractions(mediaAddProcessor);
    }

    @Test
    public void testValidateMediaUpdateNoMediaToUpdate() throws Exception {
        String jsonMessage = "{ "
                + "\"userId\": \"bobthegreat\", "
                + "\"domainFields\": {"
                + "     \"subcategoryId\": \"20001\""
                + "}, "
                + "\"comment\": \"I am the greatest image ever\""
                + "}";
        String mediaGuid = "87654321-4321-4321-4321-605040302010";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        doReturn(new ValidationStatus(true, "yup", HttpStatus.ACCEPTED.toString())).when(mediaControllerSpy).verifyUrl(anyString());
        when(mediaDBMediaDao.getMediaByGuid(eq(mediaGuid))).thenReturn(null);
        ResponseEntity<String> responseEntity = mediaControllerSpy.mediaUpdate(mediaGuid, jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("input GUID does not exist in DB"));
        verifyZeroInteractions(mediaUpdateProcessor);
        verifyZeroInteractions(mediaAddProcessor);
    }

    @Test
    public void testMediaGetSuccess() throws Exception {
        String mediaGuid = "87654321-4321-4321-4321-605040302010";
        MediaGetResponse mediaGetResponse = MediaGetResponse.builder().mediaGuid(mediaGuid).build();
        when(mediaDBMediaDao.getMediaGetResponseByGUID(eq(mediaGuid))).thenReturn(mediaGetResponse);
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.getMedia(mediaGuid, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("{\"mediaGuid\":\"87654321-4321-4321-4321-605040302010\""));
    }

    @Test
    public void testMediaGetNotAValidGuid() throws Exception {
        String mediaGuid = "not-a-valid-guid";
        MediaGetResponse mediaGetResponse = MediaGetResponse.builder().mediaGuid(mediaGuid).build();
        when(mediaDBMediaDao.getMediaGetResponseByGUID(eq(mediaGuid))).thenReturn(mediaGetResponse);
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.getMedia(mediaGuid, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Invalid media GUID provided."));
    }

    @Test
    public void testMediaGetMediaNotFound() throws Exception {
        String mediaGuid = "87654321-4321-4321-4321-605040302010";
        when(mediaDBMediaDao.getMediaGetResponseByGUID(eq(mediaGuid))).thenReturn(null);
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.getMedia(mediaGuid, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Provided media GUID does not exist."));
    }

    @Test
    public void testDeleteMediaSuccess() throws Exception {
        String mediaGuid = "87654321-4321-4321-4321-605040302010";
        Media media = Media.builder().mediaGuid(mediaGuid).lcmMediaId("12345").domain("Lodging").active("true").domainId("54321").hidden(false).build();
        when(mediaDBMediaDao.getMediaByGuid(eq(mediaGuid))).thenReturn(media);
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.deleteMedia(mediaGuid, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Media GUID 87654321-4321-4321-4321-605040302010 has been deleted successfully."));
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(argument.capture(), anyString());
        ImageMessage deleteImageMessage = argument.getValue();
        assertEquals(true, deleteImageMessage.getHidden());
    }

    @Test
    public void testDeleteMediaNotAValidGuid() throws Exception {
        String mediaGuid = "not-a-valid-guid";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.deleteMedia(mediaGuid, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Invalid media GUID provided."));
        verifyZeroInteractions(kafkaCommonPublisher);
    }

    @Test
    public void testDeleteNonExistentMedia() throws Exception {
        String mediaGuid = "87654321-4321-4321-4321-605040302010";
        when(mediaDBMediaDao.getMediaByGuid(eq(mediaGuid))).thenReturn(null);
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.deleteMedia(mediaGuid, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("input GUID does not exist in DB"));
        verifyZeroInteractions(kafkaCommonPublisher);
    }

    @Test
    public void testGetMediaByDomainIdSuccess() throws Exception {
        String domain = "lodging";
        String domainId = "1234";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        DomainIdMedia media1 = DomainIdMedia.builder().mediaGuid("12345263-1234-1234-1234-123412341234").fileName("file1.jpg").build();
        DomainIdMedia media2 = DomainIdMedia.builder().mediaGuid("12345555-1234-1234-1234-123412341234").fileName("file2.jpg").build();
        DomainIdMedia media3 = DomainIdMedia.builder().mediaGuid("12344444-1234-1234-1234-123412341234").fileName("file3.jpg").build();
        List<DomainIdMedia> mediaList = Arrays.asList(media1, media2, media3);
        MediaByDomainIdResponse mediaByDomainIdResponse = MediaByDomainIdResponse.builder()
                .domain(domain)
                .domainId(domainId)
                .totalMediaCount(mediaList.size())
                .images(mediaList).build();
        when(mediaDBMediaDao.getMediaByDomainId(Domain.LODGING, domainId, null, null, null, null, null)).thenReturn(mediaByDomainIdResponse);
        ResponseEntity<String> responseEntity = mediaControllerSpy.getMediaByDomainId(domain, domainId, null, null,
                null, null, null, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"domain\":\"lodging\",\"domainId\":\"1234\",\"totalMediaCount\":3"));
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\":\"12345263-1234-1234-1234-123412341234\""));
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\":\"12345555-1234-1234-1234-123412341234\""));
        assertTrue(responseEntity.getBody().contains("\"mediaGuid\":\"12344444-1234-1234-1234-123412341234\""));
    }

    @Test
    public void testGetMediaByDomainIdUnsupportedFilter() throws Exception {
        String domain = "lodging";
        String domainId = "1234";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.getMediaByDomainId(domain, domainId, null, null,
                "NOT_A_REAL_FILTER", null, null, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Unsupported active filter NOT_A_REAL_FILTER"));
    }

    @Test
    public void testGetMediaByDomainIdUnsupportedDomain() throws Exception {
        String domain = "RowBoats";
        String domainId = "1234";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.getMediaByDomainId(domain, domainId, null, null,
                null, null, null, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("Domain not found RowBoats"));
    }

    @Test
    public void testGetMediaByDomainIdPageSizeNullPageIndexNotNull() throws Exception {
        String domain = "lodging";
        String domainId = "1234";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.getMediaByDomainId(domain, domainId, null, 1,
                null, null, null, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("pageSize is null and pageIndex is not null, both pageSize and pageIndex parameters are inclusive. " +
                "Set both parameters or neither."));
    }

    @Test
    public void testGetMediaByDomainIdPageSizeNotNullPageIndexNull() throws Exception {
        String domain = "lodging";
        String domainId = "1234";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.getMediaByDomainId(domain, domainId, 13, null,
                null, null, null, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("pageIndex is null and pageSize is not null, both pageSize and pageIndex parameters are inclusive. " +
                "Set both parameters or neither."));
    }

    @Test
    public void testGetMediaByDomainIdPageSizeNegativePageIndexNegative() throws Exception {
        String domain = "lodging";
        String domainId = "1234";
        String requestId = "12345678-1234-1234-1234-010203040506";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = mediaControllerSpy.getMediaByDomainId(domain, domainId, -13, -1,
                null, null, null, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("pageSize and pageIndex do not accept negative values."));
    }
}
