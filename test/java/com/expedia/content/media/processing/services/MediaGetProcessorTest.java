package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.DomainIdMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.reqres.MediaByDomainIdResponse;
import com.expedia.content.media.processing.services.reqres.MediaGetResponse;
import com.expedia.content.media.processing.services.util.JSONUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MediaGetProcessorTest {
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS XXX");

    @Mock
    private MediaDao mockMediaDao;

    private MediaGetProcessor mediaGetProcessor;

    @Before
    public void initialize() {
        mediaGetProcessor = new MediaGetProcessor(mockMediaDao);
    }

    @Test
    public void processMediaGetRequestTest() throws Exception {
        Date now = new Date(System.currentTimeMillis());
        String timeStampString = DATE_FORMAT.format(now);
        String mediaGuid = "aaaabbbb-cccc-dddd-eeee-ffffgggghhhh";
        Optional<Media> media = Optional.of(Media.builder()
                .mediaGuid(mediaGuid)
                .fileUrl("s3://somewhere.com/images/1")
                .sourceUrl("s3://here")
                .fileName("hello.jpg")
                .hidden(false)
                .status("PUBLISHED")
                .domain("Lodging")
                .domainId("12345")
                .active("true")
                .width(100)
                .height(100)
                .fileSize(10001010L)
                .domainFields("{\"lcmMediaId\": \"4321\", \"subcategoryId\": \"10001\"}")
                .clientId("not samson")
                .provider("definitely not samson")
                .lastUpdated(now)
                .commentList(Arrays.asList("a cool pic", "trust me, it's cool"))
                .build());
        when(mockMediaDao.getMediaByGuid(eq(mediaGuid))).thenReturn(media);
        Optional<MediaGetResponse> response = mediaGetProcessor.processMediaGetRequest(mediaGuid);
        assertTrue(response.isPresent());
        MediaGetResponse mediaGetResponse = response.get();
        assertEquals(mediaGuid, mediaGetResponse.getMediaGuid());
        assertNotNull(mediaGetResponse.getDomainFields());
        assertTrue(2 == mediaGetResponse.getComments().size());
        assertEquals(timeStampString, mediaGetResponse.getLastUpdateDateTime());
    }

    @Test
    public void processMediaGetRequestTestMediaNotFound() throws Exception {
        String mediaGuid = "aaaabbbb-cccc-dddd-eeee-ffffgggghhhh";
        when(mockMediaDao.getMediaByGuid(eq(mediaGuid))).thenReturn(Optional.empty());
        Optional<MediaGetResponse> mediaGetResponse = mediaGetProcessor.processMediaGetRequest(mediaGuid);
        assertFalse(mediaGetResponse.isPresent());
    }

    @Test
    public void processMediaByDomainIDRequestTest() throws Exception {
        Date now = new Date(System.currentTimeMillis());
        String timeStampString = DATE_FORMAT.format(now);
        Map<String, Object> domainFields = JSONUtil.buildMapFromJson("{\"lcmMediaId\": \"4321\", \"subcategoryId\": \"10001\"}");
        List<Map<String, Object>> derivatives = JSONUtil.buildMapListFromJson("[{\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249" +
                "/9547249_7_t.jpg\", \"type\": \"t\", \"width\": 70, \"height\": 70, \"fileSize\": 2048}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_s.jpg\", \"type\": \"s\", \"width\": 200, \"height\": 138, " +
                "\"fileSize\": 8192}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_b.jpg\", \"type\": \"b\", " +
                "\"width\": 350, \"height\": 241, \"fileSize\": 22528}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_l.jpg\", \"type\": \"l\", \"width\": 255, \"height\": 144, " +
                "\"fileSize\": 11264}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_n.jpg\", \"type\": \"n\", " +
                "\"width\": 90, \"height\": 90, \"fileSize\": 3072}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_g" +
                ".jpg\", \"type\": \"g\", \"width\": 140, \"height\": 140, \"fileSize\": 7168}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_d.jpg\", \"type\": \"d\", \"width\": 180, \"height\": 180, " +
                "\"fileSize\": 10240}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_y.jpg\", \"type\": \"y\", " +
                "\"width\": 500, \"height\": 345, \"fileSize\": 43008}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_z.jpg\", \"type\": \"z\", \"width\": 1000, \"height\": 690, " +
                "\"fileSize\": 142336}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_e.jpg\", \"type\": \"e\", " +
                "\"width\": 160, \"height\": 90, \"fileSize\": 5120}]");
        List<Optional<DomainIdMedia>> domainIdMediaList = new ArrayList<>();
        Optional<DomainIdMedia> domainIdMedia0 = Optional.of(DomainIdMedia.builder()
                .mediaGuid("aaaabbbb-cccc-dddd-eeee-ffffgggghhhh")
                .fileUrl("s3://somewhere.com/images/1")
                .sourceUrl("s3://here")
                .fileName("hello.jpg")
                .status("PUBLISHED")
                .active("true")
                .width(100)
                .height(100)
                .fileSize(10001010L)
                .domainFields(domainFields)
                .derivatives(derivatives)
                .lastUpdateDateTime(timeStampString)
                .lastUpdatedBy("not samson")
                .build());
        Optional<DomainIdMedia> domainIdMedia1 = Optional.of(DomainIdMedia.builder()
                .mediaGuid("aaaabbbb-cccc-dddd-eeee-ffffgggg1111")
                .fileUrl("s3://somewhere.com/images/1")
                .sourceUrl("s3://here")
                .fileName("hello.jpg")
                .status("PUBLISHED")
                .active("true")
                .width(100)
                .height(100)
                .fileSize(10001010L)
                .domainFields(domainFields)
                .derivatives(derivatives)
                .lastUpdateDateTime(timeStampString)
                .lastUpdatedBy("not samson")
                .build());
        Optional<DomainIdMedia> domainIdMedia2 = Optional.of(DomainIdMedia.builder()
                .mediaGuid("aaaabbbb-cccc-dddd-eeee-ffffgggg2222")
                .fileUrl("s3://somewhere.com/images/1")
                .sourceUrl("s3://here")
                .fileName("hello.jpg")
                .status("PUBLISHED")
                .active("true")
                .width(100)
                .height(100)
                .fileSize(10001010L)
                .domainFields(domainFields)
                .derivatives(derivatives)
                .lastUpdateDateTime(timeStampString)
                .lastUpdatedBy("not samson")
                .build());
        domainIdMediaList.addAll(Arrays.asList(domainIdMedia0, domainIdMedia1, domainIdMedia2));
        when(mockMediaDao.getMediaByDomainId(eq(Domain.LODGING), eq("1234"), any(), any(), any(), any(), any())).thenReturn(domainIdMediaList);
        when(mockMediaDao.getTotalMediaCountByDomainId(eq(Domain.LODGING), eq("1234"), any(), any())).thenReturn(Optional.of(domainIdMediaList.size()));
        MediaByDomainIdResponse response = mediaGetProcessor.processMediaByDomainIDRequest(Domain.LODGING, "1234", null, null, null, null, null);
        assertTrue(3 == response.getImages().size());
        assertTrue(3 == response.getTotalMediaCount());
        assertEquals(Domain.LODGING.getDomain(), response.getDomain());
        assertEquals("1234", response.getDomainId());
    }

    @Test
    public void processMediaByDomainIDRequestTestNoMediaInDomainId() throws Exception {
        when(mockMediaDao.getMediaByDomainId(eq(Domain.LODGING), eq("1234"), any(), any(), any(), any(), any())).thenReturn(Arrays.asList(Optional.empty()));
        when(mockMediaDao.getTotalMediaCountByDomainId(eq(Domain.LODGING), eq("1234"), any(), any())).thenReturn(Optional.empty());
        MediaByDomainIdResponse response = mediaGetProcessor.processMediaByDomainIDRequest(Domain.LODGING, "1234", null, null, null, null, null);
        assertTrue(0 == response.getImages().size());
        assertTrue(0 == response.getTotalMediaCount());
        assertEquals(Domain.LODGING.getDomain(), response.getDomain());
        assertEquals("1234", response.getDomainId());
    }
}
