package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import org.junit.Test;

/**
 * Created by seli on 2015-07-14.
 */
public class ExpediaValidatorTest {

    @Test
    public void testValidationCategoryMessagePass() {

        ImageMessage image = new ImageMessage(null, null, "", "", "", null, new Integer(201), "123", "", "", null);
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        org.junit.Assert.assertTrue(validationStatus.isStatus());
    }

    @Test
    public void testValidationCategoryMessageFail() {

        ImageMessage image = new ImageMessage(null, null, "", "", "", null, null, "123", "", "", null);
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        org.junit.Assert.assertTrue(validationStatus.isStatus());
    }
}
