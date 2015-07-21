package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import org.junit.Assert;
import org.junit.Test;

public class RequiredValidatorTest {

    @Test
    public void testValidationMediaProviderIdMessagePass() {
        final int expedia_test_id = 23419;
        //message has mediaProviderId
        ImageMessage image = new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id)
                .categoryId("801").caption("caption").mediaProviderId("1001").build();
        RequiredValidator requiredValidator = new RequiredValidator();
        requiredValidator.setFieldName("mediaProviderId");
        ValidationStatus validationStatus = requiredValidator.validate(image);
        Assert.assertTrue(validationStatus.isValid());
    }

    @Test
    public void testValidationCategoryNumericMessageFail() {
        final int expedia_test_id = 23419;
        //message without mediaProviderId
        ImageMessage image = new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id)
                .caption("caption").build();
        RequiredValidator requiredValidator = new RequiredValidator();
        requiredValidator.setFieldName("mediaProviderId");
        ValidationStatus validationStatus = requiredValidator.validate(image);
        Assert.assertFalse(validationStatus.isValid());
    }
}
