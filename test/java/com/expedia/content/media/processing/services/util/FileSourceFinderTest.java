package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.dao.DerivativesDao;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaDerivative;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FileSourceFinderTest {

    @Mock
    private DerivativesDao mediaDBDerivativesDao;
    @Mock
    private MediaDao mediaDBMediaDao;

    FileSourceFinder fileSourceFinder = new FileSourceFinder();

    @Before
    public void initialize() throws Exception {
        String bucketName = "ewe-cs-media-test";
        String bucketPrefix = "test/source/lodging";
        String derivativeBucketPrefix = "test/derivative/lodging";
        setFieldValue(fileSourceFinder, "mediaDBDerivativesDao", mediaDBDerivativesDao);
        setFieldValue(fileSourceFinder, "mediaDBMediaDao", mediaDBMediaDao);
        setFieldValue(fileSourceFinder, "bucketName", bucketName);
        setFieldValue(fileSourceFinder, "bucketPrefix", bucketPrefix);
        setFieldValue(fileSourceFinder, "derivativeBucketPrefix", derivativeBucketPrefix);
    }

    @Test
    public void testGetFileNameFromURL() throws Exception {
        String fileName = fileSourceFinder.getFileNameFromUrl("http://images.trvl-media.com/hotels/1000000/10000/200/123/5d003ca8_e.jpg");
        assertEquals("5d003ca8_e.jpg", fileName);
    }

    @Test
    public void testMediaUrlToS3Path() throws Exception {
        String derivativeUrl = "http://images.trvl-media.com/hotels/1000000/10000/200/123/5d003ca8_e.jpg";
        String s3DerivativePath = fileSourceFinder.mediaUrlToS3Path(derivativeUrl, false);
        assertEquals("s3://ewe-cs-media-test/test/derivative/lodging/1000000/10000/200/123/5d003ca8_e.jpg", s3DerivativePath);
        String mediaUrl = "http://images.trvl-media.com/hotels/1000000/10000/200/123/5d003ca8-1234-1234-1234-010203040506.jpg";
        String s3Path = fileSourceFinder.mediaUrlToS3Path(mediaUrl, true);
        assertEquals("s3://ewe-cs-media-test/test/source/lodging/1000000/10000/200/123/5d003ca8-1234-1234-1234-010203040506.jpg", s3Path);
    }

    @Test
    public void testGetMillionFolder() throws Exception {
        String mediaUrl = "http://images.trvl-media.com/hotels/1000000/10000/200/123/5d003ca8-1234-1234-1234-010203040506.jpg";
        String folderPath = fileSourceFinder.getMillionFolderFromUrl(mediaUrl);
        assertEquals("/1000000/10000/200/123/", folderPath);
    }

    @Test
    public void testMediaByDerivativeUrlSuccess() throws Exception {
        String derivativeUrl = "http://images.trvl-media.com/hotels/1000000/10000/200/123/5d003ca8_e.jpg";

        Media media = Media.builder().mediaGuid("5d003ca8-1234-1234-1234-010203040506")
                .fileName("5d003ca8-1234-1234-1234-010203040506.jpg")
                .sourceUrl("s3://ewe-cs-media-test/test/source/lodging/1000000/10000/200/123/5d003ca8-1234-1234-1234-010203040506.jpg")
                .build();
        MediaDerivative derivative = new MediaDerivative("5d003ca8-1234-1234-1234-010203040506", "s3://ewe-cs-media-test/test/derivative/lodging/1000000/10000/200/123/5d003ca8_e.jpg",
                "e", 350, 350, 12000);
        when(mediaDBDerivativesDao.getDerivativeByLocation(eq("s3://ewe-cs-media-test/test/derivative/lodging/1000000/10000/200/123/5d003ca8_e.jpg"))).thenReturn(derivative);
        when(mediaDBMediaDao.getMediaByGuid(eq("5d003ca8-1234-1234-1234-010203040506"))).thenReturn(media);
        Media returnedMedia = fileSourceFinder.getMediaByDerivativeUrl(derivativeUrl);
        assertEquals(media, returnedMedia);
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(mediaDBDerivativesDao, times(1)).getDerivativeByLocation(argument.capture());
        assertEquals("s3://ewe-cs-media-test/test/derivative/lodging/1000000/10000/200/123/5d003ca8_e.jpg", argument.getValue());
        verify(mediaDBMediaDao, times(1)).getMediaByGuid(argument.capture());
        assertEquals("5d003ca8-1234-1234-1234-010203040506", argument.getValue());
    }
}
