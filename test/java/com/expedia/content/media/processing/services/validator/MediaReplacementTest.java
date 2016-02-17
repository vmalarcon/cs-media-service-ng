package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.Media;
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

    @Test
    public void testIsReplacement() {
        ImageMessage testMessage = ImageMessage.builder()
                .outerDomainData(
                        OuterDomain.builder()
                                .addField(MediaReplacement.REPLACE_FIELD, "true")
                                .build())
                .build();

        assertTrue(MediaReplacement.isReplacement(testMessage));
    }

    @Test
    public void testReplacementMissing() {
        ImageMessage testMessage = ImageMessage.builder()
                .mediaGuid("some-guid")
                .build();

        assertFalse(MediaReplacement.isReplacement(testMessage));
    }

    @Test
    public void testDifferentTagInDomain() {
        ImageMessage testMessage = ImageMessage.builder()
                .outerDomainData(
                        OuterDomain.builder()
                                .addField("_" + MediaReplacement.REPLACE_FIELD, "true")
                                .build())
                .build();

        assertFalse(MediaReplacement.isReplacement(testMessage));
    }

    @Test
    public void testDomainFieldsIsNull() {
        String testJson = "{ \"domainFields\": null }";
        ImageMessage testMessage = ImageMessage.parseJsonMessage(testJson);

        assertFalse(MediaReplacement.isReplacement(testMessage));
    }

    @Test
    public void testValuesOfTruth() {
        ImageMessage testMessage = ImageMessage.builder()
                .outerDomainData(
                        OuterDomain.builder()
                                .addField(MediaReplacement.REPLACE_FIELD, "TruE")
                                .build())
                .build();

        assertTrue(MediaReplacement.isReplacement(testMessage));

        testMessage = ImageMessage.builder()
                .outerDomainData(
                        OuterDomain.builder()
                                .addField(MediaReplacement.REPLACE_FIELD, "Yes")
                                .build())
                .build();

        assertFalse(MediaReplacement.isReplacement(testMessage));
    }

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
                createMedia("a", null, "true", new Date())
        ));
        assertTrue(media.isPresent());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createMedia("a", null, "false", new Date())
        ));
        assertFalse(media.isPresent());
    }

    @Test
    public void testMultipleMedia() throws Exception {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Optional<Media> media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createMedia("a", null, "true", dateFormat.parse("2016-02-17 12:00:00")),
                createMedia("b", null, "true", dateFormat.parse("2016-02-17 12:00:01")),
                createMedia("c", null, "true", dateFormat.parse("2016-02-17 12:00:02"))
        ));
        assertTrue(media.isPresent());
        assertEquals("c", media.get().getMediaGuid());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createMedia("a", null, "true", dateFormat.parse("2016-02-17 12:00:00")),
                createMedia("b", null, "true", dateFormat.parse("2016-02-17 12:00:01")),
                createMedia("c", null, "false", dateFormat.parse("2016-02-17 12:00:02"))
        ));
        assertTrue(media.isPresent());
        assertEquals("b", media.get().getMediaGuid());
        media = MediaReplacement.selectBestMedia(Lists.newArrayList(
                createMedia("a", null, "true", dateFormat.parse("2016-02-17 12:00:00")),
                createMedia("b", null, "Yes", dateFormat.parse("2016-02-17 12:00:01")),
                createMedia("c", null, "false", dateFormat.parse("2016-02-17 12:00:02"))
        ));
        assertTrue(media.isPresent());
        assertEquals("a", media.get().getMediaGuid());
    }

    public static Media createMedia(String guid, String domainId, String active, Date lastUpdated) {
        Media result = new Media();
        result.setMediaGuid(guid);
        result.setActive(active);
        result.setLastUpdated(lastUpdated);
        result.setDomainId(domainId);
        return result;
    }

}
