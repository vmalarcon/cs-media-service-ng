package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import org.junit.Test;

/**
 * Created by seli on 2015-07-14.
 */
public class NumericValidatorTest {

    @Test
    public void testValidationCategoryNumericMessagePass() {
        final int expedia_test_id = 23419;
        ImageMessage image = new ImageMessage(null, null, "", "", "", null, expedia_test_id).setCategoryId("801")
                .setCaption("caption").setMediaProviderId("1001").setCallBack(null);
        NumericValidator expediaIdValidator = new NumericValidator();
        expediaIdValidator.setFieldName("categoryId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        org.junit.Assert.assertTrue(validationStatus.isValid());
    }

    @Test
    public void testValidationCategoryNumericMessageFail() {

        ImageMessage image = new ImageMessage(null, null, "", "", "", null, null).setCategoryId("801b")
                .setCaption("caption").setMediaProviderId("1001").setCallBack(null);
        NumericValidator expediaIdValidator = new NumericValidator();
        expediaIdValidator.setFieldName("categoryId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        org.junit.Assert.assertFalse(validationStatus.isValid());
    }
}
