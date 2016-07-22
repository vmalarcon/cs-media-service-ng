package com.expedia.content.media.processing.services.validator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HTTPValidatorTest {

    @Test
    public void testFileFound() {
        assertTrue(HTTPValidator.checkFileExists("https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png").isValid());
    }

    @Test
    public void testFileNotFound() {
        assertFalse(HTTPValidator.checkFileExists("https://images.trvl-media.net/hotels/captain_potato_pants.jpg").isValid());
    }

    @Test
    public void testRedAwningUrl() {
        assertTrue(HTTPValidator.checkFileExists("https://www.redawning.com/sites/default/files/rental_property/681/coh0861-amay.jpg").isValid());
    }

    @Test
    public void testFileIsEmpty() {
        assertFalse(HTTPValidator.checkFileExists("http://photos.hotelbeds.com/giata/bigger/00/008817/008817a_hb_a_002.jpg").isValid());
    }

    @Test
    public void testSquareBracketUrl() {
        assertTrue(HTTPValidator.checkFileExists("https://assets01.redawning.com/sites/default/files/rental_property/65905/CropperCapture[6].jpg").isValid());
    }

    @Test
    public void testInvalidDomainUrl() {
        ValidationStatus status = HTTPValidator.checkFileExists("https://potatosoft.int/default/files/rental_property/65905/CropperCapture[6].jpg");
        assertFalse(status.isValid());
        assertEquals(ValidationStatus.NOT_FOUND, status.getStatus());
    }
    
    @Test
    public void testSpaceUrl() {
        assertTrue(HTTPValidator.checkFileExists("http://images.xtravelsystem.com/slide/files/public/89/0/7/9/Images/c_89079 hotel2.jpg").isValid());
    }

}
