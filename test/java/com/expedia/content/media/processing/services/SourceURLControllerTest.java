package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.services.SourceURLController;
import com.expedia.content.media.processing.services.TempDerivativeController;
import com.expedia.content.media.processing.services.ThumbnailProcessor;
import com.expedia.content.media.processing.services.dao.LcmDynamoMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.util.FileSourceFinder;
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
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration(locations = "classpath:media-services.xml")
@RunWith(MockitoJUnitRunner.class)
public class SourceURLControllerTest {
    private static final String TEST_CLIENT_ID = "a-user";

    @Mock
    private Reporting reporting;

    SourceURLController sourceURLController;

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
        sourceURLController = new SourceURLController();
    }

    @Test
    public void testSuccessfulRequest() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"http://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg\" \n"
                + "}";
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        LcmMedia lcmMedia = LcmMedia.builder().domainId(123).fileName("4600417_IMG0010.jpg").build();
        when(mockMediaDao.getContentProviderName(anyString())).thenReturn(lcmMedia);
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        FileSourceFinder fileSourceFinder = new FileSourceFinder();
        setFieldValue(sourceURLController, "fileSourceFinder", fileSourceFinder);
        setFieldValue(sourceURLController, "mediaDao", mockMediaDao);
        ResponseEntity<String> responseEntity = sourceURLController.getSourceURL(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"contentProviderMediaName\":\"4600417_IMG0010.jpg\""));
    }

    @Test
    public void testSuccessfulRequestGuid() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"http://images.trvl-media.com/hotels/11000000/10440000/10430400/10430311/8b9680cd_y.jpg\" \n"
                + "}";
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        LcmMedia lcmMedia = LcmMedia.builder().domainId(123).fileName("4600417_IMG0010.jpg").build();
        when(mockMediaDao.getContentProviderName(anyString())).thenReturn(lcmMedia);
        FileSourceFinder fileSourceFinder = mock(FileSourceFinder.class);
        String s3Location = "s3://ewe-cs-media-test/test/source/lodging/11000000/10440000/10430400/10430311/8b9680cd-f9f9-4f78-9344-2f00aba91a69.jpg";
        when(fileSourceFinder.getSourcePath(anyString(), anyString(), anyString(), anyInt())).thenReturn(s3Location);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        setFieldValue(sourceURLController, "mediaDao", mockMediaDao);
        setFieldValue(sourceURLController, "fileSourceFinder", fileSourceFinder);

        ResponseEntity<String> responseEntity = sourceURLController.getSourceURL(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"contentProviderMediaName\":\"4600417_IMG0010.jpg\""));
        assertTrue(responseEntity.getBody().contains("\"mediaSourceUrl\":\"s3://ewe-cs-media-test/test/source/lodging/11000000/10440000/10430400/10430311/8b9680cd-f9f9-4f78-9344-2f00aba91a69.jpg"));

    }

    @Test
    public void testNotFoundRequest() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"http://images.trvl-media.com/hotels/4610000/4600500/4600417/4600417_2_y.jpg\" \n"
                + "}";

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = sourceURLController.getSourceURL(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }
}
