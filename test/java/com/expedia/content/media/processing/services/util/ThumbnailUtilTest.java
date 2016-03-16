package com.expedia.content.media.processing.services.util;

import static org.junit.Assert.assertTrue;

import java.nio.file.Path;

import org.im4java.process.ProcessStarter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.expedia.content.media.processing.pipeline.domain.Metadata;
import com.expedia.content.media.processing.pipeline.util.OSDetector;

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
    public void testGeBasicMetadataWith() {
        Metadata metadata = ThumbnailUtil.getBasicMetadata(null);
        assertTrue(metadata == null);
        Path sourcePath = Mockito.mock(Path.class);
        Mockito.when(ThumbnailUtil.getBasicMetadata(sourcePath)).thenThrow(new RuntimeException());
        ThumbnailUtil.getBasicMetadata(sourcePath);
    }

    @Test
    public void testRetrieveSourcePath() {

    }

    @Test
    public void testBuildThumbnail() {

    }
}
