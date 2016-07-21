package com.expedia.content.media.processing.services.util;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class URLUtilTest {

    @Test
    public void testPatchURLwithSpace() throws Exception {
        final String urlString = "http://www.testing.com/pics/TestPic - cropped (12).jpg";
        final String value = (String) URLUtil.patchURL(urlString);
        assertEquals("http://www.testing.com/pics/TestPic%20-%20cropped%20(12).jpg", value);
    }

    @Test
    public void testNormalizeURIWithBracketInFileName() throws Exception {
        final String urlString = "https://assets01.redawning.com/sites/default/files/rental_property/65905/CropperCapture[6].jpg";
        final String value = URLUtil.normalizeURI(urlString);
        assertEquals("https://assets01.redawning.com/sites/default/files/rental_property/65905/CropperCapture%5B6%5D.jpg", value);
    }

    @Test
    public void testNormalizeURIWithBracketNoFileName() throws Exception {
        final String urlString = "https://assets01.redawning.com/sites/default/files/rental_property/[65905]/CropperCapture6.jpg";
        final String value = URLUtil.normalizeURI(urlString);
        assertEquals("https://assets01.redawning.com/sites/default/files/rental_property/[65905]/CropperCapture6.jpg", value);
    }

}
