package com.expedia.content.media.processing.services.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.reflect.FieldUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
// For faster tests, uncomment the following line
@ContextConfiguration(locations = "classpath:mvel-validator.xml")
//@ContextConfiguration(classes = Application.class)
public class EPCMVELValidatorTest {

    @BeforeClass
    public static void setUp() {
        System.setProperty("EXPEDIA_ENVIRONMENT", "test");
        System.setProperty("AWS_REGION", "us-west-2");
    }

    @Autowired
    EPCMVELValidator mvelValidator;

    @Before
    public void setUPValidator() throws Exception{
        FieldUtils.writeField(mvelValidator, "clientRule", "EPC", true);
    }
    @Test
    public void testLoadContext() {
        assertNotNull(mvelValidator);
    }

    @Test
    public void testMessageFileUrlMissing() throws Exception {
        String jsonMsg = "{}";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg.contains("fileUrl is required"));
    }

    @Test
    public void testMessageFileUrlMalformed() throws Exception {
        String jsonMsg = "{ \"fileUrl\": \"this is a malformed Url\" }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals("this is a malformed Url", imageMessage.getFileUrl());
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg.contains("fileUrl is malformed"));
    }

    @Test
    public void testMessageFileNameRequired() throws Exception {
        String jsonMsg = "{ \"fileUrl\": \"http://well-formed-url/hello\" }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg.contains("fileName is required"));
    }

    @Test
    public void testMessageMediaIdMissedAndWrongFileExtension() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals("Something", imageMessage.getFileName());
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg.contains("fileUrl extension is malformed"));
        assertTrue(errorMsg.contains("mediaGuid is required"));
    }

    @Test
    public void testMessageDomainRequired() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\"" +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals("media-uuid", imageMessage.getMediaGuid());
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg.contains("domain is required"));
    }

    @Test(expected = ImageMessageException.class)
    public void testMessageDomainInvalid() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Invalid\" " +
                        " }";
        // Parsing will fail here while converting Domain
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg.contains("domain must be 'Lodging' or 'Cars'"));
    }

    @Test
    public void testMessageDomainIdMissing() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals(Domain.LODGING, imageMessage.getOuterDomainData().getDomain());
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg, errorMsg.contains("domainId is required."));
    }

    @Test
    public void testMessageDomainIdNotNumeric() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123a\", " +
                        "    \"userId\": \"user-id\"" +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals("user-id", imageMessage.getUserId());
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg, errorMsg.contains("domainId is not numeric."));
    }

    @Test
    public void testMessageUserIdRequired() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals("123", imageMessage.getOuterDomainData().getDomainId());
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg, errorMsg.contains("userId is required."));
    }

    @Test
    public void testMessageDomainProviderRequired() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals("user-id", imageMessage.getUserId());
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg, errorMsg.contains("domainProvider is required."));
    }

    @Test
    public void testMessageValidMandatory() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"test\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals("test", imageMessage.getOuterDomainData().getProvider());
        assertEquals("test", imageMessage.getOuterDomainData().getProvider());
        assertEquals("media-uuid", imageMessage.getMediaGuid());
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        assertEquals(0, errorList.size());
    }

    @Test
    public void testMessageCategoryIdNumeric() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"test\", " +
                        "    \"domainFields\": { " +
                        "      \"category\": \"123a\" " +
                        "    } " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals("123a", imageMessage.getOuterDomainData().getDomainFieldValue("category"));
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg, errorMsg.contains("category is not numeric."));
    }

    @Test
    public void testMessagePropertyHeroNotBoolean() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"test\", " +
                        "    \"domainFields\": { " +
                        "      \"category\": \"123\", " +
                        "      \"roomHero\": \"hello\" " +
                        "    } " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals("hello", imageMessage.getOuterDomainData().getDomainFieldValue("roomHero"));
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg, errorMsg.contains("roomHero is not boolean."));
    }

    @Test
    public void testMessageRoomsIsEmpty() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"test\", " +
                        "    \"domainFields\": { " +
                        "      \"category\": \"123\", " +
                        "      \"roomHero\": \"true\", " +
                        "      \"rooms\": [ ]" +
                        "    } " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals(0, ((List) imageMessage.getOuterDomainData().getDomainFieldValue("rooms")).size());
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg, errorMsg.contains("rooms list is empty"));
    }

    @Test
    public void testMessageIsValid() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.JpEg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"rotation\": \"180\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"test\", " +
                        "    \"domainFields\": { " +
                        "      \"category\": \"123\", " +
                        "      \"roomHero\": \"true\", " +
                        "      \"rooms\": [ {} ]" +
                        "    } " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals(1, ((List) imageMessage.getOuterDomainData().getDomainFieldValue("rooms")).size());
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        assertTrue(errorList.isEmpty());
    }

    @Test
    public void testRotationIsInvalid() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.JpEg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"rotation\": \"110\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"test\", " +
                        "    \"domainFields\": { " +
                        "    } " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        assertTrue(errorList.get(0).get("error").contains("rotation accepted values are 0, 90, 180, and 270."));
    }
}
