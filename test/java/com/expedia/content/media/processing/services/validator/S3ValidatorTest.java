package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.mockito.Matchers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

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
        setFinalField(s3Validator, mockLogger);
        ValidationStatus validationStatus = s3Validator.checkFileExists("s3://ewe-cs-media-test/source/blahblahbalah.jpg");
        verify(mockLogger, times(1)).error(Matchers.<Throwable>any(), eq("s3 key query exception fileUrl = {} bucketName = {} objectName = {}"),
                eq("s3://ewe-cs-media-test/source/blahblahbalah.jpg"), eq("ewe-cs-media-test"), eq("source/blahblahbalah.jpg"));
        assertFalse(validationStatus.isValid());
    }

    //use another way to do reflection to avoid foritfy security issue.
    private void setFinalField(S3Validator s3Validator, Object newValue) throws Exception {
        Field field = FieldUtils.getField(s3Validator.getClass(), "LOGGER", true);
        FieldUtils.removeFinalModifier(field);
        field.set(null, newValue);
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
