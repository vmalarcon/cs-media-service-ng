package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MediaNamesValidatorTest {

    @Test
    public void testMediaStatusMessageValid() throws Exception {
        String validJson = "{  \n"
                + "   \"mediaNames\":[\"1037678_109010ice.jpg\",\"1055797_1742165ice.jpg\"]\n"
                + "}";
        Map<String, Object> map = ImageMessage.buildMapFromJson(validJson);
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        setFieldValue(mediaNamesValidator, "maximumRequestCount", 4);
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
    public void testMediaFileNameCountExceed() throws Exception {

        String validJson = "{  \n"
                + "   \"mediaNames\":[\"3760389_SCORE_IMG_4066.jpg\",\"test.jpg\",\"test1.jpg\",\"test2.jpg\",\"test3.jpg\"]\n"
                + "}";
        Map<String, Object> map = ImageMessage.buildMapFromJson(validJson);
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        setFieldValue(mediaNamesValidator, "maximumRequestCount", 4);
        ValidationStatus validationStatus = mediaNamesValidator.validate(map.get("mediaNames"));
        assertTrue(("messageNames count exceed the maximum 4").equals(validationStatus.getMessage()));
        assertFalse(validationStatus.isValid());
    }

    private static void setFieldValue(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Test
    public void testPropertyNameMissed() {
        String jsonWithWrongProperty = "{  \n"
                + "   \"mediaName1\":\"test.jpg\"\n"
                + "}";
        Map<String, Object> map = ImageMessage.buildMapFromJson(jsonWithWrongProperty);
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        ValidationStatus validationStatus = mediaNamesValidator.validate(map.get("mediaNames"));
        assertTrue(("message does not contain property 'messageNames'.").equals(validationStatus.getMessage()));
        assertFalse(validationStatus.isValid());
    }
}
