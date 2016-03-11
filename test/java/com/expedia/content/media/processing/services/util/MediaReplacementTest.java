package com.expedia.content.media.processing.services.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.junit.Test;

import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.util.MediaReplacement;
import com.google.common.collect.Lists;

public class MediaReplacementTest {

    @Test(expected = NullPointerException.class)
    public void testWithNullList() {
        MediaReplacement.selectBestMedia(null);
    }

    @Test
    public void testWithEmptyList() {
        Optional<Media> media = MediaReplacement.selectBestMedia(Lists.newArrayList());
        assertFalse(media.isPresent());
    }

    @Test
    public void testWithSingleMedia() {
        Optional<Media> media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", null, "true", new Date(),"456")
        ));
        assertTrue(media.isPresent());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", null, "false", new Date(),"456")
        ));
        assertFalse(media.isPresent());
    }

    @Test
    public void testMultipleMedia() throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Optional<Media> media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", null, "true", dateFormat.parse("2016-02-17 12:00:00"),"456"),
                createByFileNameMedia("b", null, "true", dateFormat.parse("2016-02-17 12:00:01"),"456"),
                createByFileNameMedia("c", null, "true", dateFormat.parse("2016-02-17 12:00:02"),"456")
        ));
        assertTrue(media.isPresent());
        assertEquals("c", media.get().getMediaGuid());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", null, "true", dateFormat.parse("2016-02-17 12:00:00"),"456"),
                createByFileNameMedia("b", null, "true", dateFormat.parse("2016-02-17 12:00:01"),"456"),
                createByFileNameMedia("c", null, "false", dateFormat.parse("2016-02-17 12:00:02"),"456")
        ));
        assertTrue(media.isPresent());
        assertEquals("b", media.get().getMediaGuid());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createByFileNameMedia("a", null, "true", dateFormat.parse("2016-02-17 12:00:00"),"456"),
                createByFileNameMedia("b", null, "Yes", dateFormat.parse("2016-02-17 12:00:01"),"456"),
                createByFileNameMedia("c", null, "false", dateFormat.parse("2016-02-17 12:00:02"),"456")
        ));
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
        return result;
    }

}
