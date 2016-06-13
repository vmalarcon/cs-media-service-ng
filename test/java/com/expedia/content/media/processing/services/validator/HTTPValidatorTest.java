package com.expedia.content.media.processing.services.validator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HTTPValidatorTest {

    @Test
    public void testFileFound() {
        assertTrue(HTTPValidator.checkFileExists("https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"));
    }

    @Test
    public void testFileNotFound() {
        assertFalse(HTTPValidator.checkFileExists("https://images.trvl-media.net/hotels/captain_potato_pants.jpg"));
    }

    @Test
    public void testRedAwningUrl() {
        assertTrue(HTTPValidator.checkFileExists("https://www.redawning.com/sites/default/files/rental_property/681/coh0861-amay.jpg"));
    }

    @Test
    public void testFileIsEmpty() {
        assertFalse(HTTPValidator.checkFileExists("http://photos.hotelbeds.com/giata/bigger/00/008817/008817a_hb_a_002.jpg"));
    }
}
