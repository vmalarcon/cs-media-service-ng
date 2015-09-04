package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MediaNamesValidatorTest {

    @Test
    public void testMediaStatusMessageValid() {
        String validJson = "{  \n"
                + "   \"mediaNames\":[\"1037678_109010ice.jpg\",\"1055797_1742165ice.jpg\"]\n"
                + "}";
        Map<String, Object> map = ImageMessage.buildMapFromJson(validJson);
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        ValidationStatus validationStatus = mediaNamesValidator.validate(map.get("mediaNames"));
        assertTrue(validationStatus.isValid());
    }

    @Test
    public void testEmptyMediaNames() {
        String validJson = "{  \n"
                + "   \"mediaNames\":[]\n"
                + "}";
        Map<String, Object> map = ImageMessage.buildMapFromJson(validJson);
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        ValidationStatus validationStatus = mediaNamesValidator.validate(map.get("mediaNames"));
        assertTrue(("messageNames value is required.").equals(validationStatus.getMessage()));
        assertFalse(validationStatus.isValid());
    }

    @Test
    public void testMessageValueIsNotArray() {
        String validJson = "{  \n"
                + "   \"mediaNames\":\"test.jpg\"\n"
                + "}";
        Map<String, Object> map = ImageMessage.buildMapFromJson(validJson);
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        ValidationStatus validationStatus = mediaNamesValidator.validate(map.get("mediaNames"));
        assertTrue(("messageNames value should be array.").equals(validationStatus.getMessage()));
        assertFalse(validationStatus.isValid());
    }

    @Test
    public void testPropertyNameMissed() {
        String validJson = "{  \n"
                + "   \"mediaName1\":\"test.jpg\"\n"
                + "}";
        Map<String, Object> map = ImageMessage.buildMapFromJson(validJson);
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        ValidationStatus validationStatus = mediaNamesValidator.validate(map.get("mediaNames"));
        assertTrue(("message does not contain property 'messageNames'.").equals(validationStatus.getMessage()));
        assertFalse(validationStatus.isValid());
    }
}
