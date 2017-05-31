package com.expedia.content.media.processing.services;

import static com.expedia.content.media.processing.pipeline.domain.Domain.LODGING;
import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import com.expedia.content.media.processing.pipeline.kafka.KafkaCommonPublisher;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBMediaDao;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaChgSproc;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.MediaLstWithCatalogItemMediaAndMediaFileNameSproc;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.CatalogItemMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.MediaUpdateDao;
import com.expedia.content.media.processing.services.dao.domain.LcmCatalogItemMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;


@ContextConfiguration(locations = "classpath:media-services.xml")
@RunWith(MockitoJUnitRunner.class)
public class MediaUpdateProcessorTest {
    @Mock
    private MediaUpdateDao mediaUpdateDao;
    @Mock
    private CatalogItemMediaDao catalogItemMediaDao;
    @Mock
    private MediaDao mediaDao;
    @Mock
    private CatalogHeroProcessor catalogHeroProcessor;
    @Mock
    private MediaDBMediaDao mediaDBMediaDao;

    private MediaUpdateProcessor mediaUpdateProcessor;

    @Mock
    private KafkaCommonPublisher kafkaCommonPublisher;

    @Before
    public void testSetUp() throws Exception {
        mediaUpdateProcessor = new MediaUpdateProcessor();
        setFieldValue(mediaUpdateProcessor, "mediaUpdateDao", mediaUpdateDao);
        setFieldValue(mediaUpdateProcessor, "catalogItemMediaDao", catalogItemMediaDao);
        setFieldValue(mediaUpdateProcessor, "mediaDao", mediaDao);
        setFieldValue(mediaUpdateProcessor, "catalogHeroProcessor", catalogHeroProcessor);
        setFieldValue(mediaUpdateProcessor, "kafkaCommonPublisher", kafkaCommonPublisher);
        setFieldValue(mediaUpdateProcessor, "mediaDBMediaDao", mediaDBMediaDao);
    }

    @Test
    public void testSubcategoryIdFieldInRQ() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\"\n"
                + "    }\n"
                + "}";

        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        Media dynamoMedia =
                new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{\"subcategoryId\":\"22003\"}",
                        new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                        new HashMap<>(), new ArrayList<>(), "", "", false, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
    }

    @Test
    public void testHeroTrueFieldInRQ() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"propertyHero\":\"true\"\n"
                + "    }\n"
                + "}";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        Media dynamoMedia =
                new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{\"propertyHero\":\"true\"}",
                        new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                        new HashMap<>(), new ArrayList<>(),
                        "", "", true, false, null);
        LcmCatalogItemMedia lcmCatalogItemMedia = mock(LcmCatalogItemMedia.class);
        when(catalogHeroProcessor.getCatalogItemMeida(12345, 123)).thenReturn(lcmCatalogItemMedia);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(catalogHeroProcessor).setMediaToHero(eq("bobthegreat"), eq(lcmCatalogItemMedia), anyBoolean(), anyString());
        verify(catalogHeroProcessor)
                .setOldCategoryForHeroPropertyMedia(any(ImageMessage.class), eq("12345"), eq("12345678-aaaa-bbbb-cccc-123456789112"), eq(123));
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString(), anyString());
    }

    @Test
    public void testSpecialTagFieldInRQ() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "    \"domain\":\"Lodging\",\n"
                 + "   \"domainFields\":{  \n"
                + "      \"propertyHero\":\"true\"\n"
                + "    }\n"
                + "}";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        Media dynamoMedia =
                new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{\"propertyHero\":\"true\"}",
                        new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                        new HashMap<>(), new ArrayList<>(),
                        "", "", true, false, null);
        LcmCatalogItemMedia lcmCatalogItemMedia = mock(LcmCatalogItemMedia.class);
        when(mediaDBMediaDao.getMediaByGuid("12345678-aaaa-bbbb-cccc-123456789112")).thenReturn(dynamoMedia);
        when(catalogHeroProcessor.getCatalogItemMeida(12345, 123)).thenReturn(lcmCatalogItemMedia);
        setFieldValue(mediaUpdateProcessor, "routeLcmPercentage", 100);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(catalogHeroProcessor, never())
                .setOldCategoryForHeroPropertyMedia(any(ImageMessage.class), eq("12345"), eq("12345678-aaaa-bbbb-cccc-123456789112"), eq(123));
        verify(catalogHeroProcessor, never()).setMediaToHero(eq("bobthegreat"), eq(lcmCatalogItemMedia), anyBoolean(), anyString());
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString(), anyString());

    }

    @Test
    public void testHeroFalseFieldInRQ() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"propertyHero\":\"false\"\n"
                + "    }\n"
                + "}";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        Media dynamoMedia =
                new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{\"propertyHero\":\"false\"}",
                        new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                        new HashMap<>(), new ArrayList<>(), "", "", false, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(catalogHeroProcessor).getCatalogItemMeida(12345, 123);
        verify(catalogHeroProcessor, never())
                .setOldCategoryForHeroPropertyMedia(any(ImageMessage.class), eq("12345"), eq("12345678-aaaa-bbbb-cccc-123456789112"), eq(123));
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString(), anyString());
    }

    @Test
    public void testHeroAndSubcategoryIdFieldsInRQ() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"subcategoryId\":\"22003\"\n"
                + "    }\n"
                + "}";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        Media dynamoMedia = new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345",
                "{\"propertyHero\":\"true\",\"subcategoryId\":\"22003\"}",
                new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), "", "", true, false, null);
        LcmCatalogItemMedia lcmCatalogItemMedia = mock(LcmCatalogItemMedia.class);
        when(catalogHeroProcessor.getCatalogItemMeida(12345, 123)).thenReturn(lcmCatalogItemMedia);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(catalogHeroProcessor).setMediaToHero(eq("bobthegreat"), eq(lcmCatalogItemMedia), anyBoolean(), anyString());
        verify(catalogHeroProcessor)
                .setOldCategoryForHeroPropertyMedia(any(ImageMessage.class), eq("12345"), eq("12345678-aaaa-bbbb-cccc-123456789112"), eq(123));
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString(), anyString());
        ArgumentCaptor<ImageMessage> imageMessage2 = ArgumentCaptor.forClass(ImageMessage.class);
    }

    @Test
    public void testActiveAndCommentFieldsInRQ() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"comment\":\"I'm a comment\"\n"
                + "}";

        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        Media dynamoMedia = new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{}",
                new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), "", "", false, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(mediaUpdateDao).updateMedia(imageMessage, 123);
        verify(catalogHeroProcessor).updateTimestamp(anyObject(), anyObject());
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString(), anyString());
    }

    @Test
    public void testRoomFieldInRQ() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"rooms\":[ \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "    }\n"
                + "}";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        //no active in imageMessage
        setFieldValue(imageMessage, "active", null);
        Media dynamoMedia = new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345",
                "{\"rooms\":[{\"roomId\":\"934779\",\"roomHero\":\"false\"},{\"roomId\":\"928675\",\"roomHero\":\"true\"}]}",
                new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), "", "", false, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(catalogItemMediaDao).getLcmRoomsByMediaId(123);
        verify(mediaUpdateDao).updateMediaTimestamp(anyObject(), anyObject());
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString(), anyString());
        ArgumentCaptor<ImageMessage> imageMessage2 = ArgumentCaptor.forClass(ImageMessage.class);
    }

    @Test
    public void testUpdateActiveFieldInDynamo() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"comment\":\"I'm a comment\"\n"
                + "}";

        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        Media dynamoMedia = Media.builder().active("false").clientId("userId").domain("Lodging").domainId("123").mediaGuid("12345678-aaaa-bbbb-cccc-123456789112").build();
        when(mediaDBMediaDao.getMediaByGuid("12345678-aaaa-bbbb-cccc-123456789112")).thenReturn(dynamoMedia);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(mediaUpdateDao).updateMedia(imageMessage, 123);
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString(), anyString());
        assertTrue(Boolean.TRUE.toString().equals(dynamoMedia.getActive()));

        String anotherJsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"comment\":\"I'm a comment\"\n"
                + "}";
        dynamoMedia = Media.builder().active("false").clientId("userId").domain("Lodging").domainId("123").mediaGuid("12345678-aaaa-bbbb-cccc-123456789112").build();
        final ImageMessage updateMessage = setActiveToNull(ImageMessage.parseJsonMessage(anotherJsonMsg));
        mediaUpdateProcessor.processRequest(updateMessage, "123", "12345", dynamoMedia);
        verify(mediaUpdateDao).updateMedia(updateMessage, 123);
        verify(kafkaCommonPublisher, times(2)).publishImageMessage(any(ImageMessage.class), anyString(), anyString());
        assertTrue(Boolean.FALSE.toString().equals(dynamoMedia.getActive()));
    }

    @Test
    public void testNonLodgingDomain() throws Exception {
        ImageMessage imageMessage = ImageMessage.builder().outerDomainData(
                OuterDomain.builder().domain(Domain.CONTENT_REPO).build()
        ).build();

        Media dynamoMedia = new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "ContentRepo", "12345",
                "{}",
                new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), "", "", false, false, null);

            mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(mediaUpdateDao, never()).updateMedia(any(ImageMessage.class), anyInt());
        verify(mediaDao).saveMedia(dynamoMedia);
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString(), anyString());
    }

    @Test
    public void testPublishingToKafka() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\"\n"
                + "    },\n"
                + "    \"comment\":\"kafkaTestLCM$#$\""
                + "}";

        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        Media dynamoMedia =
                new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{\"subcategoryId\":\"22003\"}",
                        new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                        new HashMap<>(), new ArrayList<>(), "", "", false, false, null);
        when(mediaDBMediaDao.getMediaByGuid("12345678-aaaa-bbbb-cccc-123456789112")).thenReturn(dynamoMedia);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString(), anyString());
    }

    @Test
    public void testTowRQHeroTrueFieldInRQ() throws Exception {
        MediaLstWithCatalogItemMediaAndMediaFileNameSproc mediaLstWithCatalogItemMediaAndMediaFileNameSproc = mock(MediaLstWithCatalogItemMediaAndMediaFileNameSproc.class);
        DynamoMediaRepository mediaRepo = mock(DynamoMediaRepository.class);
        CatalogItemMediaChgSproc catalogItemMediaChgSproc = mock(CatalogItemMediaChgSproc.class);
        MediaDao mediaDao = mock(MediaDao.class);
        CatalogItemMediaGetSproc catalogItemMediaGetSproc = mock(CatalogItemMediaGetSproc.class);
        CatalogHeroProcessor catalogHeroProcessor = new CatalogHeroProcessor();
        setFieldValue(catalogHeroProcessor, "mediaLstWithCatalogItemMediaAndMediaFileNameSproc", mediaLstWithCatalogItemMediaAndMediaFileNameSproc);
        setFieldValue(catalogHeroProcessor, "mediaRepo", mediaRepo);
        setFieldValue(catalogHeroProcessor, "catalogItemMediaChgSproc", catalogItemMediaChgSproc);
        setFieldValue(catalogHeroProcessor, "mediaDao", mediaDao);
        setFieldValue(catalogHeroProcessor, "catalogItemMediaGetSproc", catalogItemMediaGetSproc);
        setFieldValue(mediaUpdateProcessor, "catalogHeroProcessor", catalogHeroProcessor);
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"propertyHero\":\"true\"\n"
                + "    }\n"
                + "}";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        Media dynamoMedia =
                new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{\"propertyHero\":\"true\"}",
                        new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                        new HashMap<>(), new ArrayList<>(),
                        "", "", true, false, null);
        LcmCatalogItemMedia lcmCatalogItemMedia1 = mock(LcmCatalogItemMedia.class);
        LcmCatalogItemMedia lcmCatalogItemMedia2 = mock(LcmCatalogItemMedia.class);

        when(lcmCatalogItemMedia1.getMediaId()).thenReturn(123);
        when(lcmCatalogItemMedia1.getMediaUseRank()).thenReturn(0);
        when(lcmCatalogItemMedia1.getCatalogItemId()).thenReturn(12345);
        when(lcmCatalogItemMedia1.getLastUpdateDate()).thenReturn(new Date());

        when(lcmCatalogItemMedia2.getMediaId()).thenReturn(7777);
        when(lcmCatalogItemMedia2.getMediaUseRank()).thenReturn(0);
        when(lcmCatalogItemMedia2.getCatalogItemId()).thenReturn(12345);
        when(lcmCatalogItemMedia2.getLastUpdateDate()).thenReturn(new Date());

        when(mediaLstWithCatalogItemMediaAndMediaFileNameSproc.getMedia(12345)).thenReturn(Arrays.asList(lcmCatalogItemMedia1, lcmCatalogItemMedia2));
        when(catalogItemMediaGetSproc.getMedia(12345, 123)).thenReturn(Arrays.asList(lcmCatalogItemMedia1));
        when(mediaRepo.retrieveHeroPropertyMedia("12345", LODGING.getDomain())).thenReturn(Arrays.asList(dynamoMedia));
        // RQ 1
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        //set to hero
        verify(catalogItemMediaChgSproc).updateCategory(12345, 123, 3, "bobthegreat", "Media Service");
        verifyNoMoreInteractions(catalogItemMediaChgSproc);
        // RQ 2
        when(lcmCatalogItemMedia1.getMediaUseRank()).thenReturn(3);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        //do not unhero
        verifyZeroInteractions(mediaDao);
        verifyNoMoreInteractions(catalogItemMediaChgSproc);
        verify(kafkaCommonPublisher, times(2)).publishImageMessage(any(ImageMessage.class), anyString());
    }


    private ImageMessage setActiveToNull(final ImageMessage imageMessage) {
        ImageMessage.ImageMessageBuilder imageMessageBuilder = new ImageMessage.ImageMessageBuilder();
        imageMessageBuilder = imageMessageBuilder.transferAll(imageMessage);
        imageMessageBuilder.active(null);
        return imageMessageBuilder.build();
    }
}
