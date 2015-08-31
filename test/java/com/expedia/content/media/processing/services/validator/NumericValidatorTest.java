package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import org.junit.Test;
import org.junit.Assert;

public class NumericValidatorTest {

    @Test
    public void testValidationCategoryNumericMessagePass() {
        final int expedia_test_id = 23419;
        ImageMessage image = new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id)
                .categoryId("801").caption("caption").mediaProviderId("1001").build();
        NumericValidator expediaIdValidator = new NumericValidator();
        expediaIdValidator.setFieldName("categoryId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        Assert.assertTrue(validationStatus.isValid());
    }

    @Test
    public void testValidationCategoryNumericMessagePassWithNull() {
        final int expedia_test_id = 23419;
        ImageMessage image = new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id)
                .caption("caption").mediaProviderId("1001").build();
        NumericValidator numericValidator = new NumericValidator();
        numericValidator.setFieldName("categoryId");
        ValidationStatus validationStatus = numericValidator.validate(image);
        Assert.assertTrue(validationStatus.isValid());
    }

    @Test
    public void testValidationCategoryNumericMessageFail() {
        final int expedia_test_id = 23419;
        ImageMessage image = new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id)
                .categoryId("801b").caption("caption").mediaProviderId("1001").build();
        NumericValidator expediaIdValidator = new NumericValidator();
        expediaIdValidator.setFieldName("categoryId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        Assert.assertFalse(validationStatus.isValid());
    }
}
