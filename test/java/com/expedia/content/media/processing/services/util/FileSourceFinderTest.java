package com.expedia.content.media.processing.services.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileSourceFinderTest {
    
    @Test
    public void testGetFileNameFromURL() throws Exception {

        String fileName = FileSourceFinder.getFileNameFromUrl("http://images.trvl-media.com/hotels/1000000/10000/200/123/5d003ca8_e.jpg");
        assertTrue("5d003ca8_e.jpg".equals(fileName));
    }
    
    @Test
    public void testGenerateJsonResponse() throws Exception {
        String sourcePath = FileSourceFinder.getSourcePath("bucket", "test", "http://images.trvl-media.com/hotels/1000000/10000/200/123/5d003ca8_e.jpg",
                123);
        assertTrue("".equals(sourcePath));
    }

    @Test
    public void testGetWindowSourceUrl() throws Exception {
        String sourcePath = FileSourceFinder.getSourcePath("bucket", "test", "http://images.trvl-media.com/hotels/8000000/7010000/7001000/7000925/7000925_1_t.jpg",
                7000925);
        assertEquals("\\\\CHE-FILIDXIMG01\\GSO_MediaNew\\lodging\\8000000\\7010000\\7001000\\7000925\\7000925_1.jpg", sourcePath);
    }

    @Test
    public void testGetWindowSourceUrlSecond() throws Exception {
        String sourcePath = FileSourceFinder.getSourcePath("bucket", "test", "http://images.trvl-media.com/hotels/5000000/4610000/4600500/4600417/4600417_2_y.jpg",
                4600417);
        assertEquals("\\\\CHE-FILIDXIMG01\\GSO_media\\lodging\\5000000\\4610000\\4600500\\4600417\\4600417_2.jpg", sourcePath);
    }


}
