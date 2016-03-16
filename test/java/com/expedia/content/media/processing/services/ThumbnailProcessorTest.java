package com.expedia.content.media.processing.services;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import com.expedia.content.media.processing.services.reqres.TempDerivativeMessage;
import org.im4java.process.ProcessStarter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
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
        
        final Map<String, Object> domainDataFields = new LinkedHashMap<>();
        domainDataFields.put("categoryId", "71013");
        final OuterDomain domainData = new OuterDomain(Domain.LODGING, "1234", "Comics", "VirtualTour", domainDataFields);
        final ImageMessage message =
                ImageMessage.builder()
                        .mediaGuid("29e6394d-760a-4526-b2dd-b70d312679b7")
                        .requestId("bbbbbb-1010-bbbb-292929229")
                        .clientId("EPC")
                        .userId("you")
                        .rotation("90")
                        .active(true)
                        .fileUrl("http://i.imgur.com/Ta3uP.jpg")
                        .fileName("original_file_name.png")
                        .sourceUrl("s3://bucket/source/aaaaaaa-1010-bbbb-292929229.jpg")
                        .rejectedFolder("rejected")
                        .callback(new URL("http://multi.source.callback/callback"))
                        .comment("test comment!")
                        .outerDomainData(domainData)
                        .generateThumbnail(true)
                        .build();
                        
        String thumbnailPath = thumbProcessor.createThumbnail(message).getLocation();
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
        
        final Map<String, Object> domainDataFields = new LinkedHashMap<>();
        domainDataFields.put("categoryId", "71013");
        final OuterDomain domainData = new OuterDomain(Domain.CARS, "tx_tx", "Comics", "VirtualTour", domainDataFields);
        final ImageMessage message =
                ImageMessage.builder()
                        .mediaGuid("29e6394d-760a-4526-b2dd-b70d312679b7")
                        .requestId("bbbbbb-1010-bbbb-292929229")
                        .clientId("EPC")
                        .userId("you")
                        .rotation("90")
                        .active(true)
                        .fileUrl("http://i.imgur.com/Ta3uP.jpg")
                        .fileName("original_file_name.png")
                        .sourceUrl("s3://bucket/source/aaaaaaa-1010-bbbb-292929229.jpg")
                        .rejectedFolder("rejected")
                        .callback(new URL("http://multi.source.callback/callback"))
                        .comment("test comment!")
                        .outerDomainData(domainData)
                        .generateThumbnail(true)
                        .build();
                        
        try {
            thumbProcessor.createThumbnail(message);
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
    public void testBuildThumbnailWithThumbnailGeneration() throws Exception {
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
        
        final Map<String, Object> domainDataFields = new LinkedHashMap<>();
        domainDataFields.put("categoryId", "71013");
        final OuterDomain domainData = new OuterDomain(Domain.LODGING, "1234", "Comics", "VirtualTour", domainDataFields);
        final ImageMessage message =
                ImageMessage.builder()
                        .mediaGuid("29e6394d-760a-4526-b2dd-b70d312679b7")
                        .requestId("bbbbbb-1010-bbbb-292929229")
                        .clientId("EPC")
                        .userId("you")
                        .rotation("90")
                        .active(true)
                        .fileUrl("http://i.imgur.com/Ta3uP.jpg")
                        .fileName("original_file_name.png")
                        .sourceUrl("s3://bucket/source/aaaaaaa-1010-bbbb-292929229.jpg")
                        .rejectedFolder("rejected")
                        .callback(new URL("http://multi.source.callback/callback"))
                        .comment("test comment!")
                        .outerDomainData(domainData)
                        .generateThumbnail(true)
                        .build();
                        
        String expectedLocation =
                "https://s3-us-north-200.amazonaws.com/cs-media-bucket/test/thumbnails/lodging/1000000/10000/1300/1234/1234_29e6394d-760a-4526-b2dd-b70d312679b7_t.jpg";
        Thumbnail actualThumbnail = thumbProcessor.createThumbnail(message);
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
    
    @Test
    public void testBuildThumbnailWithoutThumbnailGeneration() throws Exception {
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
        
        final Map<String, Object> domainDataFields = new LinkedHashMap<>();
        domainDataFields.put("categoryId", "71013");
        final OuterDomain domainData = new OuterDomain(Domain.LODGING, "1234", "Comics", "VirtualTour", domainDataFields);
        final ImageMessage message =
                ImageMessage.builder()
                        .mediaGuid("29e6394d-760a-4526-b2dd-b70d312679b7")
                        .requestId("bbbbbb-1010-bbbb-292929229")
                        .clientId("EPC")
                        .userId("you")
                        .rotation("90")
                        .active(true)
                        .fileUrl("s3://bucket/source/Ta3uP.jpg")
                        .fileName("original_file_name.png")
                        .sourceUrl("s3://bucket/source/aaaaaaa-1010-bbbb-292929229.jpg")
                        .rejectedFolder("rejected")
                        .callback(new URL("http://multi.source.callback/callback"))
                        .comment("test comment!")
                        .outerDomainData(domainData)
                        .generateThumbnail(false)
                        .build();
                        
        Thumbnail actualThumbnail = thumbProcessor.createThumbnail(message);
        String thumbnailPath = actualThumbnail.getLocation();
        
        if (OSDetector.detectOS() == OSDetector.OS.WINDOWS) {
            thumbnailPath = actualThumbnail.getLocation().replace('\\', '/');
        }
        assertNull(thumbnailPath);
        assertNull(actualThumbnail.getThumbnailMetadata());
        assertNotNull(actualThumbnail.getSourceMetadata());
        assertTrue(actualThumbnail.getSourceMetadata().getHeight() > 0);
        assertTrue(actualThumbnail.getSourceMetadata().getWidth() > 0);
        assertTrue(actualThumbnail.getSourceMetadata().getFileSize() > 0);       
    }
    
}
