package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.dao.LcmDynamoMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.util.FileSourceFinder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.List;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(locations = "classpath:media-services.xml")
@RunWith(MockitoJUnitRunner.class)
public class SourceURLControllerTest {
    private static final String TEST_CLIENT_ID = "a-user";
    private static final Resource FILE_RESOURCE = new ClassPathResource("/log4j.xml");

    @Mock
    private Reporting reporting;

    @Mock
    private ResourcePatternResolver resourcePatternResolver;

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
    public void initialize() throws IllegalAccessException,NoSuchFieldException {
        sourceURLController = new SourceURLController();
        setFieldValue(sourceURLController, "resourcePatternResolver", resourcePatternResolver);

    }

    @Test
    public void testSuccessfulRequest() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"http://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg\" \n"
                + "}";
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        LcmMedia lcmMedia = LcmMedia.builder().domainId(123).fileName("4600417_IMG0010.jpg").mediaId(1234).build();
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
        LcmMedia lcmMedia = LcmMedia.builder().domainId(123).fileName("4600417_IMG0010.jpg").mediaId(1234).build();
        List<Media> mediaList = Arrays.asList(new Media().builder().mediaGuid("8b9680cd-f9f9-4f78-9344-2f00aba91a69").build());
        when(mockMediaDao.getContentProviderName(anyString())).thenReturn(lcmMedia);
        when(mockMediaDao.getMediaByMediaId(anyString())).thenReturn(mediaList);
        FileSourceFinder fileSourceFinder = mock(FileSourceFinder.class);
        String s3Location = "s3://ewe-cs-media-test/test/source/lodging/11000000/10440000/10430400/10430311/8b9680cd-f9f9-4f78-9344-2f00aba91a69.jpg";
        when(fileSourceFinder.getSourcePath(anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(s3Location);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        setFieldValue(sourceURLController, "mediaDao", mockMediaDao);
        setFieldValue(sourceURLController, "fileSourceFinder", fileSourceFinder);

        ResponseEntity<String> responseEntity = sourceURLController.getSourceURL(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"contentProviderMediaName\":\"4600417_IMG0010.jpg\""));
        assertTrue(responseEntity.getBody().contains(
                "\"mediaSourceUrl\":\"s3://ewe-cs-media-test/test/source/lodging/11000000/10440000/10430400/10430311/8b9680cd-f9f9-4f78-9344-2f00aba91a69.jpg"));

    }

    @Test
    public void testNotFoundRequest() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"http://images.trvl-media.com/hotels/4610000/4600500/4600417/4600417_2_y.jpg\" \n"
                + "}";

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getContentProviderName(anyString())).thenReturn(null);
        FileSourceFinder fileSourceFinder = mock(FileSourceFinder.class);
        String s3Location = "s3://ewe-cs-media-test/test/source/lodging/11000000/10440000/10430400/10430311/8b9680cd-f9f9-4f78-9344-2f00aba91a69.jpg";
        when(fileSourceFinder.getSourcePath(anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(s3Location);
        setFieldValue(sourceURLController, "mediaDao", mockMediaDao);
        setFieldValue(sourceURLController, "fileSourceFinder", fileSourceFinder);
        ResponseEntity<String> responseEntity = sourceURLController.getSourceURL(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test(expected = RuntimeException.class)
    public void pokeTest() throws Exception {

        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"http://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg\" \n"
                + "}";
        RuntimeException exception = new RuntimeException("this is an RuntimeException exception");
        setFieldValue(sourceURLController, "hipChatRoom", "EWE CS: Phoenix Notifications");
        Poker poker = mock(Poker.class);
        setFieldValue(sourceURLController, "poker", poker);
        MediaDao mockMediaDao = mock(LcmDynamoMediaDao.class);
        when(mockMediaDao.getContentProviderName(anyString())).thenThrow(exception);
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        FileSourceFinder fileSourceFinder = new FileSourceFinder();
        setFieldValue(sourceURLController, "fileSourceFinder", fileSourceFinder);
        setFieldValue(sourceURLController, "mediaDao", mockMediaDao);
        sourceURLController.getSourceURL(jsonMessage, mockHeader);

        verify(poker).poke(eq("Media Services failed to process a getSourceURL request - RequestId: " + requestId), eq("EWE CS: Phoenix Notifications"),
                eq(jsonMessage), eq(exception));
    }

    @Test
    public void testSuccessfulDownLoadImageRequest() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"s3://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg\" \n"
                + "}";
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        when(resourcePatternResolver.getResources(anyString()))
                .thenReturn(new Resource[]{FILE_RESOURCE});
        ResponseEntity<byte[]> responseEntity = sourceURLController.download(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().length > 0);
    }

    @Test
    public void testDownLoadSouceImageNotFound() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"s3://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg\" \n"
                + "}";
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        Resource[] resources = {createMockResource(false)};
        when(resourcePatternResolver.getResources(anyString())).thenReturn(resources);
        ResponseEntity<byte[]> responseEntity = sourceURLController.download(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void testDownLoadSouceImageMultiple() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"s3://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg\" \n"
                + "}";
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        Resource[] resources = {createMockResource(true),createMockResource(true)};
        when(resourcePatternResolver.getResources(anyString())).thenReturn(resources);
        ResponseEntity<byte[]> responseEntity = sourceURLController.download(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
    }

    private Resource createMockResource(boolean exists) {
        Resource result = mock(Resource.class);
        when(result.exists()).thenReturn(exists);
        return result;
    }

}
