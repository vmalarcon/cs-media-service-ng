package com.expedia.content.media.processing.services.util;


import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class URLUtilTest {

    @Test
    public void testPatchURLwithSpace() throws Exception {
        final String urlString = "http://www.testing.com/pics/TestPic - cropped (12).jpg";
        final String value = (String) URLUtil.patchURL(urlString);
        assertTrue("http://www.testing.com/pics/TestPic%20-%20cropped%20(12).jpg".equals(value));
    }

}
