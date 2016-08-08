package com.expedia.content.media.processing.services.util;

import com.amazonaws.services.s3.model.S3Object;
import com.expedia.content.media.processing.pipeline.util.FileImageCopy;
import com.expedia.content.media.processing.pipeline.util.ImageCopy;
import com.expedia.content.media.processing.services.testing.TestingUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FileSourceFinderTest {

    FileSourceFinder fileSourceFinder = new FileSourceFinder();

    @Before
    public void insertMocks() throws Exception {
        ImageCopy mockImageCopy = mock(ImageCopy.class);
        TestingUtil.setFieldValue(fileSourceFinder, "imageCopy", mockImageCopy);
    }

    @Test
    public void testGetFileNameFromURL() throws Exception {
        String fileName = fileSourceFinder.getFileNameFromUrl("http://images.trvl-media.com/hotels/1000000/10000/200/123/5d003ca8_e.jpg");
        assertEquals("5d003ca8_e.jpg", fileName);
    }

    @Test
    public void testGenerateJsonResponse() throws Exception {
        FileImageCopy fileImageCopy = mock(FileImageCopy.class);
        TestingUtil.setFieldValue(fileSourceFinder, "imageCopy", fileImageCopy);
        String sourcePath = fileSourceFinder.getSourcePath("bucket", "test", "http://images.trvl-media.com/hotels/1000000/10000/200/123/5d003ca8_e.jpg",
                123, "");
        assertEquals("", sourcePath);
    }

    @Test
    public void testGetWindowSourceUrlWithS3OnlyTrue() throws Exception {
        S3Object s3Object = new S3Object();
        s3Object.setBucketName("bucket");
        FileImageCopy fileImageCopy = mock(FileImageCopy.class);
        when(fileImageCopy.getImage(anyString(),anyString())).thenReturn(s3Object);
        TestingUtil.setFieldValue(fileSourceFinder, "queryS3BucketOnly", true);
        TestingUtil.setFieldValue(fileSourceFinder, "imageCopy", fileImageCopy);

        String sourcePath = fileSourceFinder.getSourcePath("bucket", "test", "http://images.trvl-media.com/hotels/8000000/7010000/7001000/7000925/7000925_1_t.jpg",
                        7000925, "");
        assertEquals("s3://bucket/test/8000000/7010000/7001000/7000925/7000925_1.jpg", sourcePath);
    }

    @Test
    public void testGetWindowSourceUrl() throws Exception {
        String sourcePath =
                fileSourceFinder.getSourcePath("bucket", "test", "http://images.trvl-media.com/hotels/8000000/7010000/7001000/7000925/7000925_1_t.jpg",
                        7000925, "");
        assertEquals("\\\\CHE-FILIDXIMG01\\GSO_MediaNew\\lodging\\8000000\\7010000\\7001000\\7000925\\7000925_1.jpg", sourcePath);
    }

    @Test
    public void testGetWindowSourceUrlSecond() throws Exception {
        String sourcePath =
                fileSourceFinder.getSourcePath("bucket", "test", "http://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg",
                        4600417, "");
        assertEquals("\\\\CHE-FILIDXIMG01\\GSO_media\\lodging\\5000000\\4610000\\4600500\\4600417\\4600417_2.jpg", sourcePath);
    }

}
