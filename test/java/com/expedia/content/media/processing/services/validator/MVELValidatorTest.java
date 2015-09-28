package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.services.Application;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.json.ImageList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
public class MVELValidatorTest {

    @Autowired
    MVELValidator mvelValidator;

    @Test
    public void testMessage() throws Exception {
        String jsonMsg = "{\n"
                + "  \"images\": [\n"
                + "    {\n"
                + "      \"fileUrl\": \"http://images.com/dir1/img1.jpg\",   \n"
                + "      \"fileName\": \"img1.jpg\",                         \n"
                + "      \"imageType\": \"Lodging22\",                        \n"
                + "      \"domainData\":                                 \n"
                + "        {\n"
                + "          \"domainDataName\": \"LCM\",                    \n"
                + "          \"domainDataFields\": {                        \n"
                + "            \"expediaId\": \"2001002a\",                  \n"
                + "            \"categoryId\": \"801\",                      \n"
                + "            \"mediaProviderId\": \"1\",                   \n"
                + "            \"propertyHero\": \"true\",                    \n"
                + "            \"caption\": \"image caption\",               \n"
                + "            \"roomId\": \"1010101a\",                      \n"
                + "            \"roomHero\": \"true\"                        \n"
                + "          }\n"
                + "        }\n"
                + "      },\n"
                + "      \n"
                + "      {\n"
                + "      \"fileUrl\": \"http://images.com/dir1/img1.jpg\",   \n"
                + "      \"fileName\": \"img2.jpg\",                         \n"
                + "      \"imageType\": \"Lodging\",                        \n"
                + "      \"domainData\":                                 \n"
                + "        {\n"
                + "          \"domainDataName\": \"LCM22\",                    \n"
                + "          \"domainDataFields\": {                        \n"
                + "            \"expediaId\": \"2001002abc\",                  \n"
                + "            \"categoryId\": \"801\",                      \n"
                + "            \"mediaProviderId\": \"1\",                   \n"
                + "            \"propertyHero\": \"true\",                    \n"
                + "            \"caption\": \"image caption\",               \n"
                + "            \"roomId\": \"101\",                      \n"
                + "            \"roomHero\": \"true\"                        \n"
                + "          }\n"
                + "        }\n"
                + "      }\n"
                + "    \n"
                + "  ]\n"
                + "}";
        ImageList imageList = JSONUtil.buildMessageListFromJson(jsonMsg);
        List<Map<String, String>> errorList = mvelValidator.validateImages(imageList.getImages());
        assertTrue(errorList.size() == 2);
    }
}
