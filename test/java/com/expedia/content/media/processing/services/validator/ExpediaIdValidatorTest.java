package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExpediaIdValidatorTest {
    
    @Test
    public void testValidationExpediaMessagePass() {
        final String expedia_test_id = "23419";
        final OuterDomain outerDomain = new OuterDomain(Domain.LODGING, expedia_test_id, "1001", null, null);
        ImageMessage image =
                new ImageMessage.ImageMessageBuilder()
                        .expediaId(Integer.parseInt(expedia_test_id))
                        .categoryId("801")
                        .caption("caption")
                        .mediaProviderId("1001")
                        .outerDomainData(outerDomain).build();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testValidationMessageWithoutExpediaIdFail() {
        final OuterDomain outerDomain = new OuterDomain(Domain.LODGING, null, "1001", "801", null);
        ImageMessage image =
                new ImageMessage.ImageMessageBuilder().categoryId("801").caption("caption").mediaProviderId("1001").outerDomainData(outerDomain).build();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        assertFalse(validationStatus.isValid());
    }
    
    @Test
    public void testValidationExpediaIdOuterDomainPass() {
        final String expedia_test_id = "23419";
        String fieldName = "expediaId";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(fieldName, new Integer(expedia_test_id));
        OuterDomain domainData = new OuterDomain(Domain.LODGING, expedia_test_id, "1001", "1e1e", dataMap);

        ImageMessage image =
                new ImageMessage.ImageMessageBuilder().outerDomainData(domainData).categoryId("801").caption("caption").mediaProviderId("1001")
                        .outerDomainData(domainData).build();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testValidationExpediaIdOuterDomainInvalidValue() {
        String fieldName = "expediaId";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(fieldName, "abcd");
        OuterDomain domainData = new OuterDomain(Domain.LODGING, null, "1001", "1e1e", dataMap);

        ImageMessage image =
                new ImageMessage.ImageMessageBuilder().outerDomainData(domainData).categoryId("801").caption("caption").mediaProviderId("1001")
                        .outerDomainData(domainData).build();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        assertFalse(validationStatus.isValid());
    }
    
    @Test
    public void testValidationExpediaIdIgnored() {
        String fieldName = "expediaId";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(fieldName, "abcd");
        OuterDomain domainData = new OuterDomain(Domain.CARS, "abcd", "1001", "1e1e", dataMap);

        ImageMessage image = new ImageMessage.ImageMessageBuilder().outerDomainData(domainData).outerDomainData(domainData).build();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        assertTrue(validationStatus.isValid());
    }
    
}
