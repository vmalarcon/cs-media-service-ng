package com.expedia.content.media.processing.services.validator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class S3ValidatorTest {

    @Test
    public void testFileFound() {
        assertTrue(S3Validator.checkFileExists("s3://ewe-cs-media-test/source/10962099_26.jpg").isValid());
    }

    @Test
    public void testFileNotFound() {
        assertFalse(S3Validator.checkFileExists("s3://ewe-cs-media-test/source/blahblahbalah.jpg").isValid());
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
