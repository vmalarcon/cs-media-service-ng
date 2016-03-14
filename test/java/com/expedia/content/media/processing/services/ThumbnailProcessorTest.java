package com.expedia.content.media.processing.services;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import com.expedia.content.media.processing.services.reqres.TempDerivativeMessage;
import org.im4java.process.ProcessStarter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;

import com.expedia.content.media.processing.pipeline.util.OSDetector;

public class ThumbnailProcessorTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Before
    public void testSetUp() throws Exception {
        if (OSDetector.detectOS() == OSDetector.OS.WINDOWS) {
            final String path = System.getenv("PATH").replace('\\', '/');
            ProcessStarter.setGlobalSearchPath(path);
        }
    }
    
    @Test
    public void testHttpLodgingImage() throws Exception {
        ThumbnailProcessor thumbProcessor = new ThumbnailProcessor();
        File workFolder = tempFolder.newFolder();
        setFieldValue(thumbProcessor, "tempWorkFolder", workFolder.getAbsolutePath());
        setFieldValue(thumbProcessor, "regionName", "us-north-200");
        setFieldValue(thumbProcessor, "thumbnailOuputLocation", "s3://cs-media-bucket/test/thumbnails/");
        
        WritableResource mockWritableResource = mock(WritableResource.class);
        when(mockWritableResource.getOutputStream()).thenReturn(new BufferedOutputStream(new FileOutputStream(tempFolder.newFile())));
        ResourceLoader mockResourceLoader = mock(ResourceLoader.class);
        when(mockResourceLoader.getResource(anyString())).thenReturn(mockWritableResource);
        setFieldValue(thumbProcessor, "resourceLoader", mockResourceLoader);
        
        String thumbnailPath =
                thumbProcessor.createThumbnail("http://i.imgur.com/Ta3uP.jpg", "29e6394d-760a-4526-b2dd-b70d312679b7", "lodging", "1234").getLocation();
                
        if (OSDetector.detectOS() == OSDetector.OS.WINDOWS) {
            thumbnailPath = thumbnailPath.replace('\\', '/');
        }
        
        assertEquals(
                "https://s3-us-north-200.amazonaws.com/cs-media-bucket/test/thumbnails/lodging/1000000/10000/1300/1234/1234_29e6394d-760a-4526-b2dd-b70d312679b7_t.jpg",
                thumbnailPath);
    }
    
    @Test
    public void testFailedImage() throws Exception {
        ThumbnailProcessor thumbProcessor = new ThumbnailProcessor();
        File workFolder = tempFolder.newFolder();
        setFieldValue(thumbProcessor, "tempWorkFolder", workFolder.getAbsolutePath());
        setFieldValue(thumbProcessor, "regionName", "us-north-200");
        setFieldValue(thumbProcessor, "thumbnailOuputLocation", "s3://cs-media-bucket/test/thumbnails/");
        
        WritableResource mockWritableResource = mock(WritableResource.class);
        when(mockWritableResource.getOutputStream()).thenThrow(new IOException());
        ResourceLoader mockResourceLoader = mock(ResourceLoader.class);
        when(mockResourceLoader.getResource(anyString())).thenReturn(mockWritableResource);
        setFieldValue(thumbProcessor, "resourceLoader", mockResourceLoader);
        
        try {
            thumbProcessor.createThumbnail("http://i.imgur.com/Ta3uP.jpg", "29e6394d-760a-4526-b2dd-b70d312679b7", "cars", "tx_tx");
            fail("Should throw exception");
        } catch (Exception e) {
            assertEquals("Unable to generate thumbnail with url: http://i.imgur.com/Ta3uP.jpg and GUID: 29e6394d-760a-4526-b2dd-b70d312679b7",
                    e.getMessage());
        }
    }
    
    @Test
    public void testCreateTempDerivativeSuccessful() throws Exception {
        ThumbnailProcessor thumbProcessor = new ThumbnailProcessor();
        File workFolder = tempFolder.newFolder();
        setFieldValue(thumbProcessor, "tempWorkFolder", workFolder.getAbsolutePath());
        setFieldValue(thumbProcessor, "regionName", "us-north-200");
        setFieldValue(thumbProcessor, "thumbnailOuputLocation", "s3://cs-media-bucket/test/thumbnails/");
        
        WritableResource mockWritableResource = mock(WritableResource.class);
        when(mockWritableResource.getOutputStream()).thenReturn(new BufferedOutputStream(new FileOutputStream(tempFolder.newFile())));
        ResourceLoader mockResourceLoader = mock(ResourceLoader.class);
        when(mockResourceLoader.getResource(anyString())).thenReturn(mockWritableResource);
        setFieldValue(thumbProcessor, "resourceLoader", mockResourceLoader);
        TempDerivativeMessage tempDerivativeMessage =
                TempDerivativeMessage.builder().height(180).width(180).fileUrl("http://i.imgur.com/Ta3uP.jpg").rotation("0").build();
        String thumbnailPath = thumbProcessor.createTempDerivativeThumbnail(tempDerivativeMessage);
        assertTrue(thumbnailPath.matches("https://s3-us-north-200.amazonaws.com/cs-media-bucket/test/thumbnails/tempderivative/(.*)"));
    }
    
    @Test
    public void testCreateTempDerivativeFailed() throws Exception {
        ThumbnailProcessor thumbProcessor = new ThumbnailProcessor();
        File workFolder = tempFolder.newFolder();
        setFieldValue(thumbProcessor, "tempWorkFolder", workFolder.getAbsolutePath());
        setFieldValue(thumbProcessor, "regionName", "us-north-200");
        setFieldValue(thumbProcessor, "thumbnailOuputLocation", "s3://cs-media-bucket/test/thumbnails/");
        
        WritableResource mockWritableResource = mock(WritableResource.class);
        when(mockWritableResource.getOutputStream()).thenReturn(new BufferedOutputStream(new FileOutputStream(tempFolder.newFile())));
        ResourceLoader mockResourceLoader = mock(ResourceLoader.class);
        when(mockResourceLoader.getResource(anyString())).thenReturn(mockWritableResource);
        setFieldValue(thumbProcessor, "resourceLoader", mockResourceLoader);
        TempDerivativeMessage tempDerivativeMessage = TempDerivativeMessage.builder().fileUrl("http://i.imgur.com/Ta3uP.jpg")
                .height(180).width(180).rotation("HELLO, IS IT ME YOU'RE LOOKING FOR?").build();
                
        try {
            thumbProcessor.createTempDerivativeThumbnail(tempDerivativeMessage);
            fail("Should throw exception");
        } catch (Exception e) {
            assertTrue(e.getMessage().matches("Unable to generate thumbnail with url: http://i.imgur.com/Ta3uP.jpg and GUID:(.*)"));
        }
    }
    
    @Test
    public void testBuildThumbnail() throws Exception {
        ThumbnailProcessor thumbProcessor = new ThumbnailProcessor();
        File workFolder = tempFolder.newFolder();
        setFieldValue(thumbProcessor, "tempWorkFolder", workFolder.getAbsolutePath());
        setFieldValue(thumbProcessor, "regionName", "us-north-200");
        setFieldValue(thumbProcessor, "thumbnailOuputLocation", "s3://cs-media-bucket/test/thumbnails/");
        
        WritableResource mockWritableResource = mock(WritableResource.class);
        when(mockWritableResource.getOutputStream()).thenReturn(new BufferedOutputStream(new FileOutputStream(tempFolder.newFile())));
        ResourceLoader mockResourceLoader = mock(ResourceLoader.class);
        when(mockResourceLoader.getResource(anyString())).thenReturn(mockWritableResource);
        setFieldValue(thumbProcessor, "resourceLoader", mockResourceLoader);
        String expectedLocation =
                "https://s3-us-north-200.amazonaws.com/cs-media-bucket/test/thumbnails/lodging/1000000/10000/1300/1234/1234_29e6394d-760a-4526-b2dd-b70d312679b7_t.jpg";
        Thumbnail actualThumbnail =
                thumbProcessor.createThumbnail("http://i.imgur.com/Ta3uP.jpg", "29e6394d-760a-4526-b2dd-b70d312679b7", "lodging", "1234");
        String thumbnailPath = actualThumbnail.getLocation();
        
        if (OSDetector.detectOS() == OSDetector.OS.WINDOWS) {
            thumbnailPath = actualThumbnail.getLocation().replace('\\', '/');
        }
        assertEquals(expectedLocation, thumbnailPath);
        assertNotNull(actualThumbnail.getThumbnailMetadata());
        assertTrue(actualThumbnail.getThumbnailMetadata().getHeight() > 0);
        assertTrue(actualThumbnail.getThumbnailMetadata().getWidth() > 0);
        assertTrue(actualThumbnail.getThumbnailMetadata().getFileSize() > 0);
        
        assertNotNull(actualThumbnail.getSourceMetadata());
        assertTrue(actualThumbnail.getSourceMetadata().getHeight() > 0);
        assertTrue(actualThumbnail.getSourceMetadata().getWidth() > 0);
        assertTrue(actualThumbnail.getSourceMetadata().getFileSize() > 0);
        
    }
    
}
