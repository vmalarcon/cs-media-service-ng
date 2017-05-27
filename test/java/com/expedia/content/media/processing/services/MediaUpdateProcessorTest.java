package com.expedia.content.media.processing.services;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import com.expedia.content.media.processing.pipeline.kafka.KafkaCommonPublisher;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBMediaDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.Media;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;


@ContextConfiguration(locations = "classpath:media-services.xml")
@RunWith(MockitoJUnitRunner.class)
public class MediaUpdateProcessorTest {
    @Mock
    private MediaDBMediaDao mediaDBMediaDao;
    @Mock
    private KafkaCommonPublisher kafkaCommonPublisher;

    private MediaUpdateProcessor mediaUpdateProcessor;


    @Before
    public void testSetUp() throws Exception {
        mediaUpdateProcessor = new MediaUpdateProcessor();
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
                + "      \"subcategoryId\":\"22001\"\n"
                + "    }\n"
                + "}";

        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        Media media =
                new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{\"subcategoryId\":\"22003\"}",
                        new Date(), "true", "EPC", "EPC", "bobtheokay", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                        new HashMap<>(), new ArrayList<>(), "", "", false, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, media);
        media.setUserId("bobthegreat");
        media.setDomainFields("{\"subcategoryId\":\"22001\"}");
        ImageMessage updatedImageMessage = media.toImageMessage();
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mediaDBMediaDao, times(1)).updateMedia(argument.capture());
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString());
        assertEquals(updatedImageMessage.getOuterDomainData().getDomainFields(), argument.getValue().getOuterDomainData().getDomainFields());
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
        Media media =
                new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{\"propertyHero\":\"false\"}",
                        new Date(), "true", "EPC", "EPC", "bobtheokay", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                        new HashMap<>(), new ArrayList<>(),
                        "", "", true, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, media);
        media.setUserId("bobthegreat");
        media.setDomainFields("{\"propertyHero\":\"true\"}");
        ImageMessage updatedImageMessage = media.toImageMessage();
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mediaDBMediaDao, times(1)).updateMedia(argument.capture());
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString());
        assertEquals(updatedImageMessage.getOuterDomainData().getDomainFields(), argument.getValue().getOuterDomainData().getDomainFields());
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
        Media media =
                new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{\"propertyHero\":\"true\"}",
                        new Date(), "true", "EPC", "EPC", "bobtheokay", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                        new HashMap<>(), new ArrayList<>(),
                        "", "", true, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, media);
        media.setUserId("bobthegreat");
        media.setDomainFields("{\"propertyHero\":\"false\"}");
        ImageMessage updatedImageMessage = media.toImageMessage();
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mediaDBMediaDao, times(1)).updateMedia(argument.capture());
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString());
        assertEquals(updatedImageMessage.getOuterDomainData().getDomainFields(), argument.getValue().getOuterDomainData().getDomainFields());
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
        Media media = new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345",
                "{\"propertyHero\":\"false\",\"subcategoryId\":\"22001\"}",
                new Date(), "true", "EPC", "EPC", "bobtheokay", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), "", "", true, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, media);
        media.setUserId("bobthegreat");
        media.setDomainFields("{\"propertyHero\":\"true\",\"subcategoryId\":\"22003\"}");
        ImageMessage updatedImageMessage = media.toImageMessage();
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mediaDBMediaDao, times(1)).updateMedia(argument.capture());
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString());
        assertEquals(updatedImageMessage.getOuterDomainData().getDomainFields(), argument.getValue().getOuterDomainData().getDomainFields());
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
        Media media = new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{}",
                new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), "", "", false, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, media);
        media.setUserId("bobthegreat");
        media.setCommentList(new ArrayList<>(Arrays.asList("I'm a comment")));
        ImageMessage updatedImageMessage = media.toImageMessage();
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mediaDBMediaDao, times(1)).updateMedia(argument.capture());
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString());
        assertEquals(updatedImageMessage.getComment(), argument.getValue().getComment());
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
        Media media = new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345",
                "{\"rooms\":[{\"roomId\":\"934777\",\"roomHero\":\"false\"}]}", new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), "", "", false, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, media);
        media.setUserId("bobthegreat");
        media.setDomainFields("{\"rooms\":[{\"roomId\":\"934779\",\"roomHero\":\"false\"},{\"roomId\":\"928675\",\"roomHero\":\"true\"}]}");
        ImageMessage updatedImageMessage = media.toImageMessage();
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mediaDBMediaDao, times(1)).updateMedia(argument.capture());
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString());
        assertEquals(updatedImageMessage.getOuterDomainData().getDomainFields(), argument.getValue().getOuterDomainData().getDomainFields());
    }

    @Test
    public void testRoomFieldmInRQAndNoRoomAssociationOriginally() throws Exception {
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
        Media media = new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345",
                "{}", new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                new HashMap<>(), new ArrayList<>(), "", "", false, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, media);
        media.setUserId("bobthegreat");
        media.setDomainFields("{\"rooms\":[{\"roomId\":\"934779\",\"roomHero\":\"false\"},{\"roomId\":\"928675\",\"roomHero\":\"true\"}]}");
        ImageMessage updatedImageMessage = media.toImageMessage();
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mediaDBMediaDao, times(1)).updateMedia(argument.capture());
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString());
        assertEquals(updatedImageMessage.getOuterDomainData().getDomainFields(), argument.getValue().getOuterDomainData().getDomainFields());
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
        Media media = Media.builder().active("false").clientId("userId").domain("Lodging").domainId("123").mediaGuid("12345678-aaaa-bbbb-cccc-123456789112").build();
        mediaUpdateProcessor.processRequest(imageMessage, media);
        media.setUserId("bobthegreat");
        media.setActive("true");
        ImageMessage updatedImageMessage = media.toImageMessage();
        ArgumentCaptor<ImageMessage> argument = ArgumentCaptor.forClass(ImageMessage.class);
        verify(mediaDBMediaDao, times(1)).updateMedia(argument.capture());
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString());
        assertEquals(updatedImageMessage.isActive(), argument.getValue().isActive());
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
        Media media =
                new Media("12345678-aaaa-bbbb-cccc-123456789112", null, null, 12L, 400, 400, "", "Lodging", "12345", "{\"subcategoryId\":\"22003\"}",
                        new Date(), "true", "EPC", "EPC", "bobthegreat", "", null, "2345145145341", "23142513425431", "", "123", new ArrayList<>(),
                        new HashMap<>(), new ArrayList<>(), "", "", false, false, null);
        mediaUpdateProcessor.processRequest(imageMessage, media);
        verify(kafkaCommonPublisher, times(1)).publishImageMessage(any(ImageMessage.class), anyString());
    }
}
