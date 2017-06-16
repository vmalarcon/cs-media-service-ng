package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class RoomValidatorTest {

    @Test
    public void testDuplicateRooms() {
        RoomValidator roomValidator = new RoomValidator();
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "     \"rooms\": [{\n"
                + "         \"roomId\": \"934777\",                      \n"
                + "        \"roomHero\": \"true\"\n"
                + "},\n"
                + "{\n"
                + "         \"roomId\": \"934777\",                      \n"
                + "        \"roomHero\": \"true\"\n"
                + "}\n"
                + "]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";

        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<String> errorList = roomValidator.validateImages(imageMessageList);
        String errorMsg = errorList.get(0);
        assertTrue(errorMsg, errorMsg.contains("There are duplicate room"));
    }
}
