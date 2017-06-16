package com.expedia.content.media.processing.services.validator;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MediaNamesValidatorTest {

    @Test
    public void testMediaStatusMessageValid() throws Exception {
        String validJson = "{  \n"
                + "   \"mediaNames\":[\"1037678_109010ice.jpg\",\"1055797_1742165ice.jpg\"]\n"
                + "}";
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        setFieldValue(mediaNamesValidator, "maximumRequestCount", 4);
        ValidationStatus validationStatus = mediaNamesValidator.validate(validJson);
        assertTrue(validationStatus.isValid());
    }

    @Test
    public void testEmptyMediaNames() {
        String validJson = "{  \n"
                + "   \"mediaNames\":[]\n"
                + "}";
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        ValidationStatus validationStatus = mediaNamesValidator.validate(validJson);
        assertTrue(("messageNames value is required.").equals(validationStatus.getMessage()));
        assertFalse(validationStatus.isValid());
    }

    @Test
    public void testMessageValueIsNotArray() {
        String validJson = "{  \n"
                + "   \"mediaNames\":\"test.jpg\"\n"
                + "}";
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        ValidationStatus validationStatus = mediaNamesValidator.validate(validJson);
        assertTrue(("messageNames value should be array.").equals(validationStatus.getMessage()));
        assertFalse(validationStatus.isValid());
    }

    @Test
    public void testMediaFileNameCountExceed() throws Exception {

        String validJson = "{  \n"
                + "   \"mediaNames\":[\"3760389_SCORE_IMG_4066.jpg\",\"test.jpg\",\"test1.jpg\",\"test2.jpg\",\"test3.jpg\"]\n"
                + "}";
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        setFieldValue(mediaNamesValidator, "maximumRequestCount", 4);
        ValidationStatus validationStatus = mediaNamesValidator.validate(validJson);
        assertTrue(("messageNames count exceed the maximum 4").equals(validationStatus.getMessage()));
        assertFalse(validationStatus.isValid());
    }

    @Test
    public void testPropertyNameMissed() {
        String jsonWithWrongProperty = "{  \n"
                + "   \"mediaName1\":\"test.jpg\"\n"
                + "}";
        MediaNamesValidator mediaNamesValidator = new MediaNamesValidator();
        ValidationStatus validationStatus = mediaNamesValidator.validate(jsonWithWrongProperty);
        assertTrue(("message does not contain property 'messageNames'.").equals(validationStatus.getMessage()));
        assertFalse(validationStatus.isValid());
    }
}
