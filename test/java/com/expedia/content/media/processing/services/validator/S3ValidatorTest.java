package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import org.junit.Test;
import org.mockito.Matchers;

import java.lang.reflect.Constructor;

import static com.expedia.content.media.processing.pipeline.util.TestingUtil.setFinalStatic;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class S3ValidatorTest {

    @Test
    public void testFileFound() {
        assertTrue(S3Validator.checkFileExists("s3://ewe-cs-media-test/source/10962099_26.jpg").isValid());
    }

    @Test public void testFileNotFound() throws Exception {
        Constructor<S3Validator> constructor = S3Validator.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        S3Validator s3Validator = constructor.newInstance();
        FormattedLogger mockLogger = mock(FormattedLogger.class);
        setFinalStatic(s3Validator.getClass().getDeclaredField("LOGGER"), mockLogger);
        ValidationStatus validationStatus = s3Validator.checkFileExists("s3://ewe-cs-media-test/source/blahblahbalah.jpg");
        verify(mockLogger, times(1)).error(Matchers.<Throwable> any(), eq("s3 key query exception fileUrl = {} bucketName = {} objectName = {}"),
                eq("s3://ewe-cs-media-test/source/blahblahbalah.jpg"), eq("ewe-cs-media-test"), eq("source/blahblahbalah.jpg"));
        assertFalse(validationStatus.isValid());
    }

    @Test
    public void testFileIsEmpty() {
        assertFalse(S3Validator.checkFileExists("s3://ewe-cs-media-test/rejected/ZeroKb.jpg").isValid());
    }

    @Test
    public void testBracket() {
        assertTrue(S3Validator.checkFileExists("s3://ewe-cs-media-test/e2e/images/Hotel-lobby-decorations[la-la-la].jpg").isValid());
    }

}
