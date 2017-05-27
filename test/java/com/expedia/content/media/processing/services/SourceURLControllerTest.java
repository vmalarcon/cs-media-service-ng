package com.expedia.content.media.processing.services;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.pipeline.util.ImageCopy;
import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.pipeline.util.S3ImageCopy;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.util.FileSourceFinder;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SourceURLControllerTest {
    private static final String TEST_CLIENT_ID = "a-user";
    private static final Resource FILE_RESOURCE = new ClassPathResource("/log4j.xml");

    @Mock
    private Reporting reporting;

    @Mock
    private ResourcePatternResolver resourcePatternResolver;

    @Mock
    private ImageCopy imageCopy;

    @Mock
    private FileSourceFinder fileSourceFinder;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private SourceURLController sourceURLController;


    @Before
    public void initialize() throws IllegalAccessException,NoSuchFieldException,IOException {
        sourceURLController = new SourceURLController();
        imageCopy = mock(S3ImageCopy.class);
        S3Object s3Object = mock(S3Object.class);
        File tmpFile = tempFolder.newFile("test");
        FileUtils.writeStringToFile(tmpFile, "Hello File");
        InputStream inputStream = new FileInputStream(tmpFile);
        HttpRequestBase httpRequestBase = mock(HttpRequestBase.class);
        S3ObjectInputStream s3ObjectInputStream = new S3ObjectInputStream(inputStream, httpRequestBase);
        when(s3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
        when(imageCopy.getImage(anyString(), anyString())).thenReturn(s3Object);
        setFieldValue(sourceURLController, "imageCopy", imageCopy);
        setFieldValue(sourceURLController, "fileSourceFinder", fileSourceFinder);

    }

    @Test
    public void testSuccessfulRequest() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"http://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg\" \n"
                + "}";
        Media media = Media.builder().mediaGuid("4600417-1234-1234-1234-010203040506")
                .fileName("4600417_IMG0010.jpg")
                .sourceUrl("s3://ewe-cs-media-test/test/source/lodging/5000000/4610000/4600500/4600417/4600417_IMG0010.jpg")
                .build();
        when(fileSourceFinder.getMediaByDerivativeUrl(eq("http://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg"))).thenReturn(media);
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> responseEntity = sourceURLController.getSourceURL(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"contentProviderMediaName\":\"4600417_IMG0010.jpg\""));
        assertTrue(responseEntity.getBody().contains("\"mediaSourceUrl\":\"s3://ewe-cs-media-test/test/source/lodging/5000000/4610000/4600500/4600417/4600417_IMG0010.jpg\""));
    }

    @Test
    public void testSuccessfulWithNoneJPGMediaRequest() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"http://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4a8a5b92_t.jpg\" \n"
                + "}";

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        Media media = Media.builder()
                .mediaGuid("4a8a5b92_real_guid_nota_fakeguid1234")
                .fileName("I_dont_end_with_a_normal_extension.10")
                .sourceUrl("s3://ewe-cs-media-test/source/5000000/4610000/4600500/4600417/4a8a5b92_real_guid_nota_fakeguid1234.10")
                .build();
        when(fileSourceFinder.getMediaByDerivativeUrl(eq("http://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4a8a5b92_t.jpg"))).thenReturn(media);
        ResponseEntity<String> responseEntity = sourceURLController.getSourceURL(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"mediaSourceUrl\":\"s3://ewe-cs-media-test/source/5000000/4610000/4600500/4600417/4a8a5b92_real_guid_nota_fakeguid1234.10\""));
        assertTrue(responseEntity.getBody().contains("\"contentProviderMediaName\":\"I_dont_end_with_a_normal_extension.10\""));
    }

    @Test
    public void testSuccessfulRequestGuid() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"http://images.trvl-media.com/hotels/11000000/10440000/10430400/10430311/8b9680cd_y.jpg\" \n"
                + "}";
        Media media = Media.builder()
                .mediaGuid("8b9680cd-f9f9-4f78-9344-2f00aba91a69")
                .fileName("4600417_IMG0010.jpg")
                .sourceUrl("s3://ewe-cs-media-test/test/source/lodging/11000000/10440000/10430400/10430311/8b9680cd-f9f9-4f78-9344-2f00aba91a69.jpg")
                .build();
        when(fileSourceFinder.getMediaByDerivativeUrl(eq("http://images.trvl-media.com/hotels/11000000/10440000/10430400/10430311/8b9680cd_y.jpg"))).thenReturn(media);
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
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
        when(fileSourceFinder.getMediaByDerivativeUrl(eq("http://images.trvl-media.com/hotels/4610000/4600500/4600417/4600417_2_y.jpg"))).thenReturn(null);
        ResponseEntity<String> responseEntity = sourceURLController.getSourceURL(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("http://images.trvl-media.com/hotels/4610000/4600500/4600417/4600417_2_y.jpg not found."));
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

        when(fileSourceFinder.getMediaByDerivativeUrl(eq("http://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg"))).thenThrow(exception);
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
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
        ResponseEntity<byte[]> responseEntity = sourceURLController.download(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().length > 0);
    }

    @Test
    public void testDownLoadSourceImageNotFound() throws Exception {
        String jsonMessage = "{ \n"
                + "  \"mediaUrl\":\"s3://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg\" \n"
                + "}";
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        when(imageCopy.getImage(anyString(), anyString())).thenReturn(null);
        ResponseEntity<byte[]> responseEntity = sourceURLController.download(jsonMessage, mockHeader);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }


    // TODO: Figure out whether to keep this test or not.
//    @Test
    public void testDownLoadSourceImageMultiple() throws Exception {
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
