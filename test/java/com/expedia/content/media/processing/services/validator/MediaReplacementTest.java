package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.Media;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MediaReplacementTest {

    @Test
    public void testIsReplacement() throws Exception {
        MediaReplacement mediaReplacement = createMediaReplacement("A, B, Some Provider, Some Other");

        ImageMessage testMessage = ImageMessage.builder()
                .outerDomainData(
                        OuterDomain.builder()
                                .mediaProvider("Some Provider")
                                .build())
                .build();

        assertTrue(mediaReplacement.isReplacement(testMessage));
    }

    @Test
    public void testProviderMissing() throws Exception {
        MediaReplacement mediaReplacement = createMediaReplacement("A, B, Some Provider, Some Other");

        ImageMessage testMessage = ImageMessage.builder()
                .mediaGuid("some-guid")
                .build();

        assertFalse(mediaReplacement.isReplacement(testMessage));
    }

    @Test
    public void testDifferentTagInDomain() throws Exception {
        MediaReplacement mediaReplacement = createMediaReplacement("A, B, Some Provider, Some Other");

        ImageMessage testMessage = ImageMessage.builder()
                .outerDomainData(
                        OuterDomain.builder()
                                .mediaProvider("SomeProvider")
                                .build())
                .build();

        assertFalse(mediaReplacement.isReplacement(testMessage));
    }

    @Test
    public void testDifferentCaseTagInDomain() throws Exception {
        MediaReplacement mediaReplacement = createMediaReplacement("A, B, Some Provider, Some Other");

        ImageMessage testMessage = ImageMessage.builder()
                .outerDomainData(
                        OuterDomain.builder()
                                .mediaProvider("some provider")
                                .build())
                .build();

        assertFalse(mediaReplacement.isReplacement(testMessage));
    }

    @Test
    public void testProviderFieldIsNull() throws Exception {
        MediaReplacement mediaReplacement = createMediaReplacement("A, B, Some Provider, Some Other");
        ImageMessage testMessage = ImageMessage.parseJsonMessage("{ \"domain\": \"Lodging\", \"domainProvider\": null }");

        assertNull(testMessage.getOuterDomainData().getProvider());
        assertFalse(mediaReplacement.isReplacement(testMessage));

        testMessage = ImageMessage.parseJsonMessage("{ \"domain\": \"Lodging\" }");

        assertNull(testMessage.getOuterDomainData().getProvider());
        assertFalse(mediaReplacement.isReplacement(testMessage));
    }

    @Test
    public void testProviderFieldInMessage() throws Exception {
        MediaReplacement mediaReplacement = createMediaReplacement("A, B, Some Provider, Some Other");
        ImageMessage testMessage = ImageMessage.parseJsonMessage("{ \"domain\": \"Lodging\", \"domainProvider\": \"Some Provider\" }");

        assertEquals("Some Provider", testMessage.getOuterDomainData().getProvider());
        assertTrue(mediaReplacement.isReplacement(testMessage));
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

    private static MediaReplacement createMediaReplacement(final String providers) throws Exception {
        return new MediaReplacement(providers);
    }
}
