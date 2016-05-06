package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.dao.domain.Media;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

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
        Optional<Media> media = MediaReplacement.selectBestMedia(Lists.newArrayList(), "456", "SCORE");
        assertFalse(media.isPresent());
    }

    @Test
    public void testWithSingleMedia() {
        Optional<Media> media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "true", new Date(), "456")
        ), "456", "SCORE");
        assertTrue(media.isPresent());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "false", new Date(), "456")
        ), "456", "SCORE");
        assertFalse(media.isPresent());
    }

    @Test
    public void testWithSingleMediaWithDifferentDomain() {
        Optional<Media> media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "true", new Date(), "456")
        ), "456", "SCORE");
        assertTrue(media.isPresent());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "123", "true", new Date(), "456")
        ), "456", "SCORE");
        assertFalse(media.isPresent());
    }

    @Test
    public void testMultipleMedia() throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Optional<Media> media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "true", dateFormat.parse("2016-02-17 12:00:00"), "456"),
                createByFileNameMedia("b", "456", "true", dateFormat.parse("2016-02-17 12:00:01"), "456"),
                createByFileNameMedia("c", "456", "true", dateFormat.parse("2016-02-17 12:00:02"), "456")
        ), "456", "SCORE");
        assertTrue(media.isPresent());
        assertEquals("c", media.get().getMediaGuid());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "true", dateFormat.parse("2016-02-17 12:00:00"), "456"),
                createByFileNameMedia("b", "456", "true", dateFormat.parse("2016-02-17 12:00:01"), "456"),
                createByFileNameMedia("c", "456", "false", dateFormat.parse("2016-02-17 12:00:02"), "456")
        ), "456", "SCORE");
        assertTrue(media.isPresent());
        assertEquals("b", media.get().getMediaGuid());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", "456", "true", dateFormat.parse("2016-02-17 12:00:00"), "456"),
                createByFileNameMedia("b", "456", "Yes", dateFormat.parse("2016-02-17 12:00:01"), "456"),
                createByFileNameMedia("c", "456", "false", dateFormat.parse("2016-02-17 12:00:02"), "456")
        ), "456", "SCORE");
        assertTrue(media.isPresent());
        assertEquals("a", media.get().getMediaGuid());
    }

    public static Media createByFileNameMedia(String guid, String domainId, String active, Date lastUpdated, String lcmMediaId) {
        Media result = new Media();
        result.setMediaGuid(guid);
        result.setLcmMediaId(lcmMediaId);
        result.setActive(active);
        result.setLastUpdated(lastUpdated);
        result.setDomainId(domainId);
        result.setProvider("SCORE");
        return result;
    }

}
