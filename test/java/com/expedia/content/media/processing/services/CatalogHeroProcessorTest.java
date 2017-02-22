package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.LcmCatalogItemMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaChgSproc;
import com.expedia.content.media.processing.services.dao.sql.MediaLstWithCatalogItemMediaAndMediaFileNameSproc;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CatalogHeroProcessorTest {

    @Mock
    private DynamoMediaRepository mediaRepo;
    @Mock
    private MediaDao mediaDao;
    @Mock
    private MediaLstWithCatalogItemMediaAndMediaFileNameSproc mediaLstWithCatalogItemMediaAndMediaFileNameSproc;
    @Mock
    private CatalogItemMediaChgSproc catalogItemMediaChgSproc;

    private CatalogHeroProcessor catalogHeroProcessor;

    @Before
    public void testSetUp() throws Exception {
        catalogHeroProcessor = new CatalogHeroProcessor();
        setFieldValue(catalogHeroProcessor, "mediaRepo", mediaRepo);
        setFieldValue(catalogHeroProcessor, "mediaLstWithCatalogItemMediaAndMediaFileNameSproc", mediaLstWithCatalogItemMediaAndMediaFileNameSproc);
        setFieldValue(catalogHeroProcessor, "mediaDao", mediaDao);
        setFieldValue(catalogHeroProcessor, "catalogItemMediaChgSproc", catalogItemMediaChgSproc);
    }

    @Test
    public void testCompareDatesDynamoMediaMoreRecentUpdate() throws Exception {
        Long datePastMillis = System.currentTimeMillis() - (8*60*60*1000);
        Date datePast = new Date(datePastMillis);
        Date dateNow = new Date(System.currentTimeMillis());
        Media dynamoMedia = Media.builder()
                .active("true")
                .propertyHero(true)
                .clientId("userId")
                .domain("Lodging")
                .domainId("123")
                .lcmMediaId("12345")
                .domainFields("{\"propertyHero\":\"true\", \"subcategoryId\":\"20000\"}")
                .mediaGuid("12345678-aaaa-bbbb-cccc-123456789112")
                .lastUpdated(dateNow)
                .build();
        Media dynamoMediaNewHero = Media.builder()
                .active("true")
                .propertyHero(false)
                .clientId("userId")
                .domain("Lodging")
                .domainId("123")
                .lcmMediaId("12346")
                .mediaGuid("12345678-aaaa-bbbb-cccc-123456789324")
                .lastUpdated(datePast)
                .build();
        LcmCatalogItemMedia lcmMedia = LcmCatalogItemMedia.builder()
                .catalogItemId(123)
                .lastUpdateDate(datePast)
                .mediaUseRank(3)
                .mediaId(12345)
                .build();
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "   \"domain\":\"Lodging\",\n"
                + "   \"domainFields\": {\n"
                + "     \"subcategoryId\": \"801\",\n"
                + "     \"propertyHero\": \"true\"\n"
                + "  },"
                + "   \"comment\":\"I'm a comment\"\n"
                + "}";

        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        when(mediaRepo.retrieveHeroPropertyMedia("123", "Lodging")).thenReturn(Arrays.asList(dynamoMedia, dynamoMediaNewHero));
        when(mediaLstWithCatalogItemMediaAndMediaFileNameSproc.getMedia(123)).thenReturn(Arrays.asList(lcmMedia));
        catalogHeroProcessor.setOldCategoryForHeroPropertyMedia(imageMessage, "123", "12345678-aaaa-bbbb-cccc-123456789324", 12346);
        verify(catalogItemMediaChgSproc, times(1)).updateCategory(123, 12345, 20000, "bobthegreat", "Media Service");
    }

    @Test
    public void testCompareDatesLCMMediaMoreRecentUpdate() throws Exception {
        Long datePastMillis = System.currentTimeMillis() - (9*60*60*1000);
        Date datePast = new Date(datePastMillis);
        Date dateNow = new Date(System.currentTimeMillis());
        Media dynamoMedia = Media.builder()
                .active("true")
                .propertyHero(true)
                .clientId("userId")
                .domain("Lodging")
                .domainId("123")
                .lcmMediaId("12345")
                .domainFields("{\"propertyHero\":\"true\", \"subcategoryId\":\"20000\"}")
                .mediaGuid("12345678-aaaa-bbbb-cccc-123456789112")
                .lastUpdated(datePast)
                .build();
        Media dynamoMediaNewHero = Media.builder()
                .active("true")
                .propertyHero(false)
                .clientId("userId")
                .domain("Lodging")
                .domainId("123")
                .lcmMediaId("12346")
                .mediaGuid("12345678-aaaa-bbbb-cccc-123456789324")
                .lastUpdated(dateNow)
                .build();
        LcmCatalogItemMedia lcmMedia = LcmCatalogItemMedia.builder()
                .catalogItemId(123)
                .lastUpdateDate(dateNow)
                .mediaUseRank(40000)
                .mediaId(12345)
                .build();
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "   \"domain\":\"Lodging\",\n"
                + "   \"domainFields\": {\n"
                + "     \"subcategoryId\": \"801\",\n"
                + "     \"propertyHero\": \"true\"\n"
                + "  },"
                + "   \"comment\":\"I'm a comment\"\n"
                + "}";

        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        when(mediaRepo.retrieveHeroPropertyMedia("123", "Lodging")).thenReturn(Arrays.asList(dynamoMedia, dynamoMediaNewHero));
        when(mediaLstWithCatalogItemMediaAndMediaFileNameSproc.getMedia(123)).thenReturn(Arrays.asList(lcmMedia));
        catalogHeroProcessor.setOldCategoryForHeroPropertyMedia(imageMessage, "123", "12345678-aaaa-bbbb-cccc-123456789324", 12346);
        verify(catalogItemMediaChgSproc, never()).updateCategory(anyInt(), anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    public void testCompareDatesLCMMediaMoreRecentUpdateWithMediaUseRank3() throws Exception {
        Long datePastMillis = System.currentTimeMillis() - (9*60*60*1000);
        Date datePast = new Date(datePastMillis);
        Date dateNow = new Date(System.currentTimeMillis());
        Media dynamoMedia = Media.builder()
                .active("true")
                .propertyHero(true)
                .clientId("userId")
                .domain("Lodging")
                .domainId("123")
                .lcmMediaId("12345")
                .domainFields("{\"propertyHero\":\"true\", \"subcategoryId\":\"20000\"}")
                .mediaGuid("12345678-aaaa-bbbb-cccc-123456789112")
                .lastUpdated(datePast)
                .build();
        Media dynamoMediaNewHero = Media.builder()
                .active("true")
                .propertyHero(false)
                .clientId("userId")
                .domain("Lodging")
                .domainId("123")
                .lcmMediaId("12346")
                .mediaGuid("12345678-aaaa-bbbb-cccc-123456789324")
                .lastUpdated(dateNow)
                .build();
        LcmCatalogItemMedia lcmMedia = LcmCatalogItemMedia.builder()
                .catalogItemId(123)
                .lastUpdateDate(dateNow)
                .mediaUseRank(3)
                .mediaId(12345)
                .build();
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "   \"domain\":\"Lodging\",\n"
                + "   \"domainFields\": {\n"
                + "     \"subcategoryId\": \"801\",\n"
                + "     \"propertyHero\": \"true\"\n"
                + "  },"
                + "   \"comment\":\"I'm a comment\"\n"
                + "}";

        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        when(mediaRepo.retrieveHeroPropertyMedia("123", "Lodging")).thenReturn(Arrays.asList(dynamoMedia, dynamoMediaNewHero));
        when(mediaLstWithCatalogItemMediaAndMediaFileNameSproc.getMedia(123)).thenReturn(Arrays.asList(lcmMedia));
        catalogHeroProcessor.setOldCategoryForHeroPropertyMedia(imageMessage, "123", "12345678-aaaa-bbbb-cccc-123456789324", 12346);
        verify(catalogItemMediaChgSproc, times(1)).updateCategory(123, 12345, 20000, "bobthegreat", "Media Service");
    }
}
