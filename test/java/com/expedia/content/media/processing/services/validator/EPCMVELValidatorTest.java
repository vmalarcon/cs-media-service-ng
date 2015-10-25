package com.expedia.content.media.processing.services.validator;

import static org.junit.Assert.assertTrue;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.Application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
public class EPCMVELValidatorTest {

    @BeforeClass
    public static void setUp(){
        System.setProperty("EXPEDIA_ENVIRONMENT","test");
        System.setProperty("AWS_REGION","us-west-2");

    }
    @Autowired
    EPCMVELValidator mvelValidator;

    @Test
    public void testMessageWithNonNumbericId() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"fileUrl\":\"s3://ewe-cs-media-test/source/testImage.jpg\",\n"
                + "   \"fileName\":\"img1.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n"
                + "   \"domainName\":\"LCM\",\n"
                + "   \"domainId\":\"154a\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"categoryId\":\"801a\",\n"
                + "      \"mediaProviderId\":\"1\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"caption\":\"image caption\",\n"
                + "      \"roomId\":\"1010101a\",\n"
                + "      \"roomHero\":\"true\"\n"
                + "   }\n"
                + "}";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);

        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorList.size() == 1);
        assertTrue(errorMsg.contains("domainDataId is not numeric"));
        assertTrue(errorMsg.contains("categoryId is not numeric"));
        assertTrue(errorMsg.contains("roomId is not numeric"));
    }

    @Test
    public void testMessageMissedFileName() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"fileUrl\":\"s3://ewe-cs-media-test/source/testImage.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n"
                + "   \"domainName\":\"LCM\",\n"
                + "   \"domainId\":\"154a\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"categoryId\":\"801a\",\n"
                + "      \"mediaProviderId\":\"1\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"caption\":\"image caption\",\n"
                + "      \"roomId\":\"1010101a\",\n"
                + "      \"roomHero\":\"true\"\n"
                + "   }\n"
                + "}";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorList.size() == 1);
        assertTrue(errorMsg.contains("domainDataId is not numeric"));
        assertTrue(errorMsg.contains("categoryId is not numeric"));
        assertTrue(errorMsg.contains("roomId is not numeric"));
        assertTrue(errorMsg.contains("fileName is required."));
    }

    @Test
    public void testMessageDomainNameWrong() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"fileUrl\":\"s3://ewe-cs-media-test/source/testImage.jpg\",\n"
                + "   \"fileName\":\"img1.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n"
                + "   \"domainName\":\"LCM2\",\n"
                + "   \"domainId\":\"154\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"mediaProviderId\":\"1\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"caption\":\"image caption\",\n"
                + "      \"roomId\":\"1010101\",\n"
                + "      \"roomHero\":\"true\"\n"
                + "   }\n"
                + "}";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg.contains("domainDataName must be LCM."));
    }
}
