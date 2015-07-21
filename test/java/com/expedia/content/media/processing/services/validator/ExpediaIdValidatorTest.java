package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.domain.ImageType;
import org.junit.Test;

public class ExpediaIdValidatorTest {

    @Test
    public void testValidationExpediaMessagePass() {
        final int expedia_test_id = 23419;
        ImageMessage image = new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id)
                .categoryId("801").caption("caption").mediaProviderId("1001").build();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        org.junit.Assert.assertTrue(validationStatus.isValid());
    }

    @Test
    public void testValidationMessageWithoutExpediaIdFail() {
        ImageMessage image =
                new ImageMessage.ImageMessageBuilder().categoryId("801").caption("caption").mediaProviderId("1001").imageType(ImageType.LODGING).build();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        org.junit.Assert.assertFalse(validationStatus.isValid());
    }
}
