package com.expedia.content.media.processing.services;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.services.validator.TempDerivativeMVELValidator;

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
                + "\"rotation\": \"90\", " + "\"width\": 180, " + "\"height\": 180" + "}";
        TempDerivativeMVELValidator tempDerivativeMVELValidator = mock(TempDerivativeMVELValidator.class);
        when(tempDerivativeMVELValidator.validateTempDerivativeMessage(any())).thenReturn("");
        setFieldValue(tempDerivativeController, "tempDerivativeMVELValidator", tempDerivativeMVELValidator);

        ThumbnailProcessor thumbnailProcessor = mock(ThumbnailProcessor.class);
        String thumbnailUrl = "http://url.net/thumbnail.jpg";
        when(thumbnailProcessor.createThumbnail(any(ImageMessage.class)).getLocation()).thenReturn(thumbnailUrl);
        setFieldValue(tempDerivativeController, "thumbnailProcessor", thumbnailProcessor);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
      
        ResponseEntity<String> responseEntity = tempDerivativeController.getTempDerivative(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"thumbnailUrl\":"));
    }
}
