package com.expedia.content.media.processing.services.validator;

import static org.junit.Assert.*;

import org.junit.Test;

public class HTTPValidatorTest {

    @Test
    public void testFileFound() {
        assertTrue(HTTPValidator.checkFileExists("https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"));
    }

    @Test
    public void testFileNotFound() {
        assertFalse(HTTPValidator.checkFileExists("https://images.trvl-media.net/hotels/captain_potato_pants.jpg"));
    }

}
