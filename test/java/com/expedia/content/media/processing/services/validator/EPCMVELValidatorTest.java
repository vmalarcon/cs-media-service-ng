package com.expedia.content.media.processing.services.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;
import com.expedia.content.media.processing.services.Application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:mvel-validator.xml")
public class EPCMVELValidatorTest {

    @BeforeClass
    public static void setUp() {
        System.setProperty("EXPEDIA_ENVIRONMENT", "test");
        System.setProperty("AWS_REGION", "us-west-2");
    }

    @Autowired
    EPCMVELValidator mvelValidator;

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
    public void testMessageMediaIdRequired() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg.contains("mediaId is required"));
    }

    @Test
    public void testMessageDomainRequired() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaId\": \"media-uuid\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
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
                        "    \"mediaId\": \"media-uuid\", " +
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
                        "    \"mediaId\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg, errorMsg.contains("domainId is required."));
    }

    @Ignore
    @Test
    public void testMessageDomainIdNotNumeric() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaId\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123a\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        System.out.println(imageMessage.getOuterDomainData().getDomainId());
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
                        "    \"mediaId\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg, errorMsg.contains("userId is required."));
    }

    @Ignore
    @Test
    public void testMessageDomainProviderRequired() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaId\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"test\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        assertEquals("test", imageMessage.getOuterDomainData().getProvider());
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg, errorMsg.contains("domainProvider is required."));
    }

    @Test
    public void testMessageValid() throws Exception {
        String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaId\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"test\" " +
                        " }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        assertEquals(0, errorList.size());
    }
}
