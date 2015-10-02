package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.services.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
public class EPCMVELValidatorTest {

    @Autowired
    EPCMVELValidator mvelValidator;

    @Test
    public void testMessageWithNonNumbericId() throws Exception {
        String jsonMsg = "{\n"
                + "      \"fileUrl\": \"http://images.com/dir1/img1.jpg\",   \n"
                + "      \"fileName\": \"img1.jpg\",                         \n"
                + "      \"imageType\": \"Lodging\",                        \n"
                + "      \"domainData\":[                                 \n"
                + "        {\n"
                + "          \"domainDataName\": \"LCM\",                    \n"
                + "          \"domainDataFields\": {                        \n"
                + "            \"expediaId\": \"2001002a\",                  \n"
                + "            \"categoryId\": \"801a\",                      \n"
                + "            \"mediaProviderId\": \"1\",                   \n"
                + "            \"propertyHero\": \"true\",                    \n"
                + "            \"caption\": \"image caption\",               \n"
                + "            \"roomId\": \"1010101a\",                      \n"
                + "            \"roomHero\": \"true\"                        \n"
                + "          }\n"
                + "        }\n"
                + "    ]\n"
                + "      }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);

        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorList.size() == 1);
        assertTrue(errorMsg.contains("expediaid is not numeric"));
        assertTrue(errorMsg.contains("categoryId is not numeric"));
        assertTrue(errorMsg.contains("roomId is not numeric"));
    }

    @Test
    public void testMessageMissedFileName() throws Exception {
        String jsonMsg = "{\n"
                + "      \"fileUrl\": \"http://images.com/dir1/img1.jpg\",   \n"
                + "      \"imageType\": \"Lodging\",                        \n"
                + "      \"domainData\":[                                 \n"
                + "        {\n"
                + "          \"domainDataName\": \"LCM\",                    \n"
                + "          \"domainDataFields\": {                        \n"
                + "            \"expediaId\": \"2001002a\",                  \n"
                + "            \"categoryId\": \"801a\",                      \n"
                + "            \"mediaProviderId\": \"1\",                   \n"
                + "            \"propertyHero\": \"true\",                    \n"
                + "            \"caption\": \"image caption\",               \n"
                + "            \"roomId\": \"1010101a\",                      \n"
                + "            \"roomHero\": \"true\"                        \n"
                + "          }\n"
                + "        }\n"
                + "    ]\n"
                + "      }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);

        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorList.size() == 1);
        assertTrue(errorMsg.contains("expediaid is not numeric"));
        assertTrue(errorMsg.contains("categoryId is not numeric"));
        assertTrue(errorMsg.contains("roomId is not numeric"));
        assertTrue(errorMsg.contains("fileName is required."));

    }

    @Test
    public void testMessageMissedId() throws Exception {
        String jsonMsg = "{\n"
                + "      \"fileUrl\": \"http://images.com/dir1/img1.jpg\",   \n"
                + "      \"imageType\": \"Lodging\",                        \n"
                + "      \"domainData\":[                                 \n"
                + "        {\n"
                + "          \"domainDataName\": \"LCM\",                    \n"
                + "          \"domainDataFields\": {                        \n"
                + "            \"mediaProviderId\": \"1\",                   \n"
                + "            \"propertyHero\": \"true\",                    \n"
                + "            \"caption\": \"image caption\",               \n"
                + "            \"roomId\": \"1010101a\",                      \n"
                + "            \"roomHero\": \"true\"                        \n"
                + "          }\n"
                + "        }\n"
                + "    ]\n"
                + "      }";
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageMessageList);

        String errorMsg = errorList.get(0).get("error");
        assertTrue(errorMsg.contains("categoryId is required"));


    }
}
