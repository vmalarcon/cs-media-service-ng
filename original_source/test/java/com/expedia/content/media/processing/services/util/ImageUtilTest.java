package com.expedia.content.media.processing.services.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;

import org.im4java.process.ProcessStarter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.util.IOUtils;
import com.expedia.content.media.processing.pipeline.domain.Metadata;
import com.expedia.content.media.processing.pipeline.util.OSDetector;
import com.expedia.content.media.processing.services.testing.TestingUtil;

@RunWith(MockitoJUnitRunner.class)
public class ImageUtilTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

  
    @Test
    public void testGeBasicMetadataWith() throws Exception {
        File fileName = TestingUtil.buildTestImage(500, 500, tempFolder.newFile("source.jpg"));
        Metadata metadata = ImageUtil.getBasicImageMetadata(Paths.get(fileName.getAbsolutePath()));
        assertNotNull(metadata);
        assertEquals(new Integer(500), metadata.getWidth());
        assertEquals(new Integer(500), metadata.getHeight());

        File tempFile = tempFolder.newFile();
        InputStream inputStream = new ByteArrayInputStream("hello".getBytes());
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        IOUtils.copy(inputStream, outputStream);
        assertNull(ImageUtil.getBasicImageMetadata(Paths.get(tempFile.getAbsolutePath())));

        assertNull(ImageUtil.getBasicImageMetadata(null));
    }
}
