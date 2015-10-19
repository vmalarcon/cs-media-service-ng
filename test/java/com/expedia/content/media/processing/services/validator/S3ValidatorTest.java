package com.expedia.content.media.processing.services.validator;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by seli on 2015-10-19.
 */
public class S3ValidatorTest {

    @Test
    public void testS3ValidationFalse() {
        boolean fileExist = S3Validator.checkFileExists("s3://ewe-cs-media-test/source/testImage3344.jpg");
        assertTrue(fileExist == false);
    }
}
