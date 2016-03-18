package com.expedia.content.media.processing.services.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.im4java.process.ProcessStarter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.expedia.content.media.processing.pipeline.domain.Metadata;
import com.expedia.content.media.processing.pipeline.util.OSDetector;
import com.expedia.content.media.processing.pipeline.util.TemporaryWorkFolder;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;

@RunWith(MockitoJUnitRunner.class)
public class ThumbnailUtilTest {

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
    public void testGeBasicMetadataWith() throws Exception {
        File fileName = ImageUtil.buildTestImage(500, 500, tempFolder.newFile("source.jpg"));
        Metadata metadata = ThumbnailUtil.getBasicMetadata(Paths.get(fileName.getAbsolutePath()));
        assertNotNull(metadata);
        assertEquals(new Integer(500), metadata.getWidth());
        assertEquals(new Integer(500), metadata.getHeight());
        assertNull(ThumbnailUtil.getBasicMetadata(null));
    }

    @Test
    public void testRetrieveSourcePath() throws Exception {
        String url = "http://url.com/file.jpg";
        String guid = "4554-789-4512-693";
        Path sourcePath = mock(Path.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource resource = mock(Resource.class);
        TemporaryWorkFolder workFolder = mock(TemporaryWorkFolder.class);
        when(resourceLoader.getResource(url)).thenReturn(resource);
        File folderName = tempFolder.newFolder("folder");
        when(sourcePath.toString()).thenReturn(folderName.getAbsolutePath());
        when(workFolder.getWorkPath()).thenReturn(sourcePath);
        InputStream inputStream = new ByteArrayInputStream("hello".getBytes());
        when(resource.getInputStream()).thenReturn(inputStream);
        Path retrievePath = ThumbnailUtil.retrieveSourcePath(url, guid, workFolder, resourceLoader);
        assertNotNull(retrievePath);
        verify(resourceLoader, times(1)).getResource(eq(url));
        verify(workFolder, times(1)).getWorkPath();
        verify(resource, times(1)).getInputStream();
    }

    @Test
    public void testBuildThumbnail() throws Exception {
        File thumbnailFile = ImageUtil.buildTestImage(70, 70, tempFolder.newFile("guid_t.jpg"));
        File sourceFile = ImageUtil.buildTestImage(500, 500, tempFolder.newFile("source.jpg"));
        String url = "https://cs-media-bucket/guid_t.jpg";
        Path sourcePath = Paths.get(sourceFile.getAbsolutePath());
        Path thumbnailPath = Paths.get(thumbnailFile.getAbsolutePath());
        Thumbnail thumbnail = ThumbnailUtil.buildThumbnail(thumbnailPath, url, sourcePath);
        assertNotNull(thumbnail);
        assertEquals(url, thumbnail.getLocation());
        assertEquals(new Integer(500), thumbnail.getSourceMetadata().getHeight());
        assertEquals(new Integer(500), thumbnail.getSourceMetadata().getWidth());
        assertEquals(new Integer(70), thumbnail.getThumbnailMetadata().getHeight());
        assertEquals(new Integer(70), thumbnail.getThumbnailMetadata().getWidth());

        thumbnail = ThumbnailUtil.buildThumbnail(null, null, sourcePath);
        assertNull(thumbnail.getThumbnailMetadata());
        assertEquals(new Integer(500), thumbnail.getSourceMetadata().getHeight());
        assertEquals(new Integer(500), thumbnail.getSourceMetadata().getWidth());
    }
}
