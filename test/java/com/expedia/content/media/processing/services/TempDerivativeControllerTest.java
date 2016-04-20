package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import com.expedia.content.media.processing.services.validator.TempDerivativeMVELValidator;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(locations = "classpath:media-services.xml")
@RunWith(MockitoJUnitRunner.class)
public class TempDerivativeControllerTest {
    private static final String TEST_CLIENT_ID = "a-user";

    @Mock
    private Reporting reporting;
    @Mock
    private QueueMessagingTemplate queueMessagingTemplateMock;

    TempDerivativeController tempDerivativeController;

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
        tempDerivativeController = new TempDerivativeController();
    }

    @Test
    public void testSuccessfulTemporaryDerivativeRequest() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg/why/would/someone/name/all/of/their/files/original.jpg\", "
                + "\"rotation\": 90, " + "\"width\": 180, " + "\"height\": 180" + "}";
        TempDerivativeMVELValidator tempDerivativeMVELValidator = mock(TempDerivativeMVELValidator.class);
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");
        setFieldValue(tempDerivativeController, "tempDerivativeMVELValidator", tempDerivativeMVELValidator);
        
        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);       
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);
        setFieldValue(tempDerivativeController, "thumbnailProcessor", thumbnailProcessor);

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
                + "\"rotation\": 90, " + "\"width\": 180, " + "\"height\": 180" + "}";
        TempDerivativeMVELValidator tempDerivativeMVELValidator = mock(TempDerivativeMVELValidator.class);
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");
        setFieldValue(tempDerivativeController, "tempDerivativeMVELValidator", tempDerivativeMVELValidator);

        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);
        setFieldValue(tempDerivativeController, "thumbnailProcessor", thumbnailProcessor);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = tempDerivativeController.getTempDerivative(jsonMessage, mockHeader);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

    }

    @Test
    public void testTemporaryDerivativeRequestNotFound() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://i.imgurdd.com/3PRGFii.jpg/why/would/someone/name/all/of/their/files/original.jpg\", "
                + "\"rotation\": 90, " + "\"width\": 180, " + "\"height\": 180" + "}";
        TempDerivativeMVELValidator tempDerivativeMVELValidator = mock(TempDerivativeMVELValidator.class);
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");
        setFieldValue(tempDerivativeController, "tempDerivativeMVELValidator", tempDerivativeMVELValidator);

        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);
        setFieldValue(tempDerivativeController, "thumbnailProcessor", thumbnailProcessor);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = tempDerivativeController.getTempDerivative(jsonMessage, mockHeader);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());

    }

    @Test
    public void testTemporaryDerivativeInvalidRequest() throws Exception {
        String jsonMessage = " " + "\"fileUrl\": \"http://i.imgurdd.com/3PRGF.jpg/why/would/someone/name/all/of/their/files/original.jpg\", "
                + "\"rotation\": 90, " + "\"width\": 180, " + "\"height\": 180" + "}";
        TempDerivativeMVELValidator tempDerivativeMVELValidator = mock(TempDerivativeMVELValidator.class);
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");
        setFieldValue(tempDerivativeController, "tempDerivativeMVELValidator", tempDerivativeMVELValidator);

        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);
        setFieldValue(tempDerivativeController, "thumbnailProcessor", thumbnailProcessor);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = tempDerivativeController.getTempDerivative(jsonMessage, mockHeader);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());

    }

    @Test
    public void testTemporaryDerivativeS3URLwithSpaceRequest() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"s3://ewe-cs-media-test/e2e/images/Space Test.jpg\", "
                + "\"rotation\": 90, " + "\"width\": 180, " + "\"height\": 180" + "}";
        TempDerivativeMVELValidator tempDerivativeMVELValidator = mock(TempDerivativeMVELValidator.class);
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");
        setFieldValue(tempDerivativeController, "tempDerivativeMVELValidator", tempDerivativeMVELValidator);

        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);
        setFieldValue(tempDerivativeController, "thumbnailProcessor", thumbnailProcessor);

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
    public void testTemporaryDerivativeHTTPURLwithSpaceRequest() throws Exception {
        String jsonMessage = "{ " + "\"fileUrl\": \"http://images.xtravelsystem.com/slide/files/public//88/8/0/3/Images/lobby (2) (Custom).jpg\", "
                + "\"rotation\": 90, " + "\"width\": 180, " + "\"height\": 180" + "}";
        TempDerivativeMVELValidator tempDerivativeMVELValidator = mock(TempDerivativeMVELValidator.class);
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");
        setFieldValue(tempDerivativeController, "tempDerivativeMVELValidator", tempDerivativeMVELValidator);

        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getLocation()).thenReturn(thumbnailUrl);
        when(thumbnailProcessor.createThumbnail(any())).thenReturn(thumbnail);
        setFieldValue(tempDerivativeController, "thumbnailProcessor", thumbnailProcessor);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> responseEntity = tempDerivativeController.getTempDerivative(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"thumbnailUrl\":"));
        verify(thumbnailProcessor,times(1)).createTempDerivativeThumbnail(any());
    }
}
