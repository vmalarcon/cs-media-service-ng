package com.expedia.content.media.processing.services;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

        String thumbnailPath = thumbProcessor.createThumbnail("http://i.imgur.com/Ta3uP.jpg", "29e6394d-760a-4526-b2dd-b70d312679b7", "lodging", "1234");
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
            assertEquals("Unable to generate thumbnail with url: http://i.imgur.com/Ta3uP.jpg and GUID: 29e6394d-760a-4526-b2dd-b70d312679b7", e.getMessage());
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
        TempDerivativeMessage tempDerivativeMessage = new TempDerivativeMessage("http://i.imgur.com/Ta3uP.jpg", "0", 180, 180);
        String thumbnailPath = thumbProcessor.createTempDerivativeThumbnail(tempDerivativeMessage);
        assertTrue(thumbnailPath.matches("https://s3-us-north-200.amazonaws.com/cs-media-bucket/test/thumbnails/(.*)"));
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
        TempDerivativeMessage tempDerivativeMessage = new TempDerivativeMessage("http://i.imgur.com/Ta3uP.jpg", "HELLO, IS IT ME YOU'RE LOOKING FOR?", 180, 180);

        try {
            thumbProcessor.createTempDerivativeThumbnail(tempDerivativeMessage);
            fail("Should throw exception");
        } catch (Exception e) {
            assertEquals("Unable to generate thumbnail with url: http://i.imgur.com/Ta3uP.jpg", e.getMessage());
        }
    }
}
