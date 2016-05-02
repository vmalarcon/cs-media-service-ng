package com.expedia.content.media.processing.services;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.CatalogItemMediaDao;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.MediaUpdateDao;
import com.expedia.content.media.processing.services.dao.domain.LcmCatalogItemMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;

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

    private MediaUpdateProcessor mediaUpdateProcessor;

    @Before
    public void testSetUp() throws Exception {
        mediaUpdateProcessor = new MediaUpdateProcessor();
        setFieldValue(mediaUpdateProcessor, "mediaUpdateDao", mediaUpdateDao);
        setFieldValue(mediaUpdateProcessor, "catalogItemMediaDao", catalogItemMediaDao);
        setFieldValue(mediaUpdateProcessor, "mediaDao", mediaDao);
        setFieldValue(mediaUpdateProcessor, "catalogHeroProcessor", catalogHeroProcessor);
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
                        new HashMap<>(), new ArrayList<>(), "", "", false, Boolean.FALSE.toString());
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
                        "", "", true, Boolean.FALSE.toString());
        LcmCatalogItemMedia lcmCatalogItemMedia = mock(LcmCatalogItemMedia.class);
        when(catalogHeroProcessor.getCatalogItemMeida(12345, 123)).thenReturn(lcmCatalogItemMedia);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(catalogHeroProcessor).setMediaToHero(eq("bobthegreat"), eq(lcmCatalogItemMedia), anyBoolean(), anyString());
        verify(catalogHeroProcessor)
                .setOldCategoryForHeroPropertyMedia(any(ImageMessage.class), eq("12345"), eq("12345678-aaaa-bbbb-cccc-123456789112"), eq(123));

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
                        new HashMap<>(), new ArrayList<>(), "", "", false, Boolean.FALSE.toString());
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(catalogHeroProcessor).getCatalogItemMeida(12345, 123);
        verify(catalogHeroProcessor, never())
                .setOldCategoryForHeroPropertyMedia(any(ImageMessage.class), eq("12345"), eq("12345678-aaaa-bbbb-cccc-123456789112"), eq(123));

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
                new HashMap<>(), new ArrayList<>(), "", "", true, Boolean.FALSE.toString());
        LcmCatalogItemMedia lcmCatalogItemMedia = mock(LcmCatalogItemMedia.class);
        when(catalogHeroProcessor.getCatalogItemMeida(12345, 123)).thenReturn(lcmCatalogItemMedia);
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(catalogHeroProcessor).setMediaToHero(eq("bobthegreat"), eq(lcmCatalogItemMedia), anyBoolean(), anyString());
        verify(catalogHeroProcessor)
                .setOldCategoryForHeroPropertyMedia(any(ImageMessage.class), eq("12345"), eq("12345678-aaaa-bbbb-cccc-123456789112"), eq(123));
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
                new HashMap<>(), new ArrayList<>(), "", "", false, Boolean.FALSE.toString());
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(mediaUpdateDao).updateMedia(imageMessage, 123);
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
        Media dynamoMedia = new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345",
                "{\"rooms\":[{\"roomId\":\"934779\",\"roomHero\":\"false\"},{\"roomId\":\"928675\",\"roomHero\":\"true\"}]}",
                new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), "", "", false, Boolean.FALSE.toString());
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);
        verify(catalogItemMediaDao).getLcmRoomsByMediaId(123);
    }

    @Test
    public void testHideRecords() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "    \"hidden\":\"true\",\n"
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
        Media dynamoMedia = new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345",
                "{\"rooms\":[{\"roomId\":\"934779\",\"roomHero\":\"false\"},{\"roomId\":\"928675\",\"roomHero\":\"true\"}]}",
                new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), "", "", false, Boolean.FALSE.toString());
        mediaUpdateProcessor.processRequest(imageMessage, "123", "12345", dynamoMedia);       
        assertEquals(Boolean.TRUE.toString(), dynamoMedia.getHidden());
    }

}
