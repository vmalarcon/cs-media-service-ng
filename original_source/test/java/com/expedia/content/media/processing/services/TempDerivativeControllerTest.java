package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import com.expedia.content.media.processing.services.reqres.TempDerivativeMessage;
import com.expedia.content.media.processing.services.validator.TempDerivativeMVELValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.MultiValueMap;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(locations = "classpath:media-services.xml")
@RunWith(MockitoJUnitRunner.class)
public class TempDerivativeControllerTest {
    private static final String TEST_CLIENT_ID = "a-user";

    @Mock
    private TempDerivativeMVELValidator tempDerivativeMVELValidator;
    @Mock
    private ThumbnailProcessor thumbnailProcessor;
    @Mock
    private Poker poker;

    private TempDerivativeController tempDerivativeController;

    @Before
    public void setSecurityContext() throws IllegalAccessException {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(TEST_CLIENT_ID);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Before
    public void initialize() throws IllegalAccessException {
        tempDerivativeController = new TempDerivativeController(thumbnailProcessor, tempDerivativeMVELValidator, poker);
    }

    @Test
    public void testSuccessfulTemporaryDerivativeRequest() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg/why/would/someone/name/all/of/their/files/original.jpg\", "
                + "\"rotation\": \"90\", " + "\"width\": 180, " + "\"height\": 180" + "}";
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");

        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);       
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
      
        ResponseEntity<String> responseEntity = tempDerivativeController.getTempDerivative(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"thumbnailUrl\":"));
        verify(thumbnailProcessor,times(1)).createTempDerivativeThumbnail(any());
    }

    @Test
    public void testBadTemporaryDerivativeRequest() throws Exception {
        String jsonMessage = "{ "
                + "\"rotation\": \"90\", " + "\"width\": 180, " + "\"height\": 180" + "}";
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");

        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = tempDerivativeController.getTempDerivative(jsonMessage, mockHeader);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

    }

    @Test
    public void testTemporaryDerivativeRequestNotFound() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgurdd.com/3PRGFii.jpg/why/would/someone/name/all/of/their/files/original.jpg\", "
                + "\"rotation\": \"90\", " + "\"width\": 180, " + "\"height\": 180" + "}";
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");

        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = tempDerivativeController.getTempDerivative(jsonMessage, mockHeader);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

    }

    @Test
    public void testTemporaryDerivativeRequestZeroBytes() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgurdd.com/3PRGFii.jpg/why/would/someone/name/all/of/their/files/original.jpg\", "
                + "\"rotation\": \"90\", " + "\"width\": 180, " + "\"height\": 180" + "}";
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");

        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        TempDerivativeController tempDerivativeControllerSpy = spy(tempDerivativeController);
        doReturn(new ValidationStatus(false, "0 Bytes", "0 Bytes")).when(tempDerivativeControllerSpy).verifyUrl(anyString());
        ResponseEntity<String> responseEntity = tempDerivativeControllerSpy.getTempDerivative(jsonMessage, mockHeader);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testTemporaryDerivativeInvalidRequest() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgurdd.com/3PRGF.jpg/why/would/someone/name/all/of/their/files/original.jpg\", "
                + "\"rotation\": \"90\", " + "\"width\": 180, " + "\"height\": 180" + "}";
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");

        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = tempDerivativeController.getTempDerivative(jsonMessage, mockHeader);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void testTemporaryDerivativeInvalidJSONRequest() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgurdd.com/3PRGF.jpg/why/would/someone/name/all/of/their/files/original.jpg\", "
                + "\"rotation\": \"90\", " + "\"width\": 180, " + "\"height\": 180" + "}";
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("whoa! your JSON is badly formatted!");

        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = tempDerivativeController.getTempDerivative(jsonMessage, mockHeader);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("JSON request format is invalid. "));
    }

    @Test(expected = RuntimeException.class)
    public void pokeTestGetTempDerivative() throws Exception {
        Poker poker = mock(Poker.class);
        setFieldValue(tempDerivativeController, "poker", poker);
        setFieldValue(tempDerivativeController, "hipChatRoom", "EWE CS: Phoenix Notifications");
        String jsonMessage = "{ " + "\"fileUrl\": \"http://www.hoteldavanzati.it/wp/wp-content/uploads/2011/10/hotel-davanzati-10-02-10-472_small.jpg\", "
                + "\"rotation\": \"90\", " + "\"width\": 180, " + "\"height\": 180" + "}";

        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");

        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        RuntimeException exception = new RuntimeException("this is a runtime exception");
        when(thumbnailProcessor.createTempDerivativeThumbnail(any(TempDerivativeMessage.class))).thenThrow(exception);
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        when(tempDerivativeController.getTempDerivative(jsonMessage, mockHeader)).thenThrow(exception);
        tempDerivativeController.getTempDerivative(jsonMessage, mockHeader);
        verify(poker).poke(eq("Media Services failed to process a getTempDerivative request - RequestId: " + requestId),  eq("EWE CS: Phoenix Notifications"),
                eq(jsonMessage), eq(exception));
    }

}
