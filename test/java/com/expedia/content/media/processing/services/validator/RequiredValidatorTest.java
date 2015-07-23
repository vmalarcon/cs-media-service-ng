package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
public class RequiredValidatorTest {

    @Test
    public void testMediaProviderIdMessagePass() {
        final int expedia_test_id = 23419;
        ImageMessage image = new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id)
                .categoryId("801").caption("caption").mediaProviderId("1001").build();
        RequiredValidator requiredValidator = new RequiredValidator();
        requiredValidator.setFieldName("mediaProviderId");
        ValidationStatus validationStatus = requiredValidator.validate(image);
        assertTrue(validationStatus.isValid());
    }

    @Test
    public void testWithoutMediaProIdFail() {
        final int expedia_test_id = 23419;
        ImageMessage image = new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id)
                .caption("caption").build();
        RequiredValidator requiredValidator = new RequiredValidator();
        requiredValidator.setFieldName("mediaProviderId");
        ValidationStatus validationStatus = requiredValidator.validate(image);
        assertFalse(validationStatus.isValid());
    }
}
