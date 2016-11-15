package com.expedia.content.media.processing.services.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

public class HTTPValidatorTest {

    private ValidationStatus status;
    // used to release the mocked HTTP_CLIENT and use the real one
    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();

    @Test
    public void testFileFound() {
        ValidationStatus status = HTTPValidator.checkFileExists("https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png");
        assertTrue(status.isValid());
        assertEquals(ValidationStatus.VALID, status.getStatus());
    }

    @Test
    public void testFileNotFound() {
        ValidationStatus status =  HTTPValidator.checkFileExists("https://images.trvl-media.net/hotels/captain_potato_pants.jpg");
        assertFalse(status.isValid());
        assertEquals(ValidationStatus.NOT_FOUND, status.getStatus());
    }

    @Test
    public void testRedAwningUrl() {
        ValidationStatus status = HTTPValidator.checkFileExists("https://www.redawning.com/sites/default/files/rental_property/681/coh0861-amay.jpg");
        assertTrue(status.isValid());
        assertEquals(ValidationStatus.VALID, status.getStatus());
    }

    @Test
    public void testSquareBracketUrl() {
        ValidationStatus status = HTTPValidator.checkFileExists("https://assets01.redawning.com/sites/default/files/rental_property/65905/CropperCapture[6].jpg");
        assertTrue(status.isValid());
        assertEquals(ValidationStatus.VALID, status.getStatus());
    }

    @Test
    public void testInvalidDomainUrl() {
        ValidationStatus status = HTTPValidator.checkFileExists("https://potatosoft.int/default/files/rental_property/65905/CropperCapture[6].jpg");
        assertFalse(status.isValid());
        assertEquals(ValidationStatus.NOT_FOUND, status.getStatus());
    }
    
    @Test
    public void testSpaceUrl() {
        ValidationStatus status = HTTPValidator.checkFileExists("http://images.xtravelsystem.com/slide/files/public/89/0/7/9/Images/c_89079 hotel2.jpg");
        assertTrue(status.isValid());
        assertEquals(ValidationStatus.VALID, status.getStatus());
    }
}
