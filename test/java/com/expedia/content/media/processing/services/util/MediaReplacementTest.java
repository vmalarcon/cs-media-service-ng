package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.dao.domain.Media;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MediaReplacementTest {

    @Test(expected = NullPointerException.class)
    public void testWithNullList() {
        MediaReplacement.selectBestMedia(null, "456", "SCORE");
    }

    @Test
    public void testWithEmptyList() {
        Media media = MediaReplacement.selectBestMedia(Lists.newArrayList(), "456", "SCORE");
        assertFalse(media == null);
    }

    @Test
    public void testWithSingleMedia() {
        Media media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "true", new Date(), "456")
        ), "456", "SCORE");
        assertTrue(media == null);
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "false", new Date(), "456")
        ), "456", "SCORE");
        assertTrue(media == null);
    }

    @Test
    public void testWithSingleMediaWithDifferentDomain() {
        Media media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "true", new Date(), "456")
        ), "456", "SCORE");
        assertTrue(media == null);
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "123", "true", new Date(), "456")
        ), "456", "SCORE");
        assertFalse(media == null);
    }

    @Test
    public void testWithGuidNameMediaWithDifferentProvider() {
        Media media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByGuidFileNameMedia("a", "456", "true", new Date(), "456", "918492_EPCInternalUser_22dcf000-f34c-4eee-81f0-f971d48cd8e8.jpeg")
        ), "456", "VFML");
        assertTrue(media == null);
    }

    @Test
    public void testWithNameMediaWithDifferentProvider() {
        Media media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByGuidFileNameMedia("a", "456", "true", new Date(), "456", "918492_epc_testdfd.jpeg")
        ), "456", "VFML");
        assertFalse(media == null);
    }

    @Test
    public void testMultipleMedia() throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Media media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "true", dateFormat.parse("2016-02-17 12:00:00"), "456"),
                createByFileNameMedia("b", "456", "true", dateFormat.parse("2016-02-17 12:00:01"), "456"),
                createByFileNameMedia("c", "456", "true", dateFormat.parse("2016-02-17 12:00:02"), "456")
        ), "456", "SCORE");
        assertTrue(media == null);
        assertEquals("c", media.getMediaGuid());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "true", dateFormat.parse("2016-02-17 12:00:00"), "456"),
                createByFileNameMedia("b", "456", "true", dateFormat.parse("2016-02-17 12:00:01"), "456"),
                createByFileNameMedia("c", "456", "false", dateFormat.parse("2016-02-17 12:00:02"), "456")
        ), "456", "SCORE");
        assertTrue(media == null);
        assertEquals("c", media.getMediaGuid());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "true", dateFormat.parse("2016-02-17 12:00:00"), "456"),
                createByFileNameMedia("b", "456", "Yes", dateFormat.parse("2016-02-17 12:00:01"), "456"),
                createByFileNameMedia("c", "456", "false", dateFormat.parse("2016-02-17 12:00:02"), "456")
        ), "456", "SCORE");
        assertTrue(media == null);
        assertEquals("c", media.getMediaGuid());
    }

    public static Media createByFileNameMedia(String guid, String domainId, String active, Date lastUpdated, String lcmMediaId) {
        Media result = new Media();
        result.setMediaGuid(guid);
        result.setLcmMediaId(lcmMediaId);
        result.setActive(active);
        result.setLastUpdated(lastUpdated);
        result.setDomainId(domainId);
        result.setProvider("SCORE");
        result.setFileName("123_test.jpg");
        return result;
    }

    public static Media createByGuidFileNameMedia(String guid, String domainId, String active, Date lastUpdated, String lcmMediaId, String fileName) {
        Media result = new Media();
        result.setMediaGuid(guid);
        result.setLcmMediaId(lcmMediaId);
        result.setActive(active);
        result.setLastUpdated(lastUpdated);
        result.setDomainId(domainId);
        result.setProvider("SCORE");
        result.setFileName(fileName);
        return result;
    }

}
