package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomainData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class NumericValidatorTest {
    
    @Test
    public void testValidationCategoryNumericMessagePass() {
        final int expedia_test_id = 23419;
        ImageMessage image =
                new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id).categoryId("801").caption("caption").mediaProviderId("1001").build();
        NumericValidator expediaIdValidator = new NumericValidator();
        expediaIdValidator.setFieldName("categoryId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        Assert.assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testValidationCategoryNumericMessagePassWithNull() {
        final int expedia_test_id = 23419;
        ImageMessage image = new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id).caption("caption").mediaProviderId("1001").build();
        NumericValidator numericValidator = new NumericValidator();
        numericValidator.setFieldName("categoryId");
        ValidationStatus validationStatus = numericValidator.validate(image);
        Assert.assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testValidationCategoryNumericMessageFail() {
        final int expedia_test_id = 23419;
        ImageMessage image =
                new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id).categoryId("801b").caption("caption").mediaProviderId("1001").build();
        NumericValidator expediaIdValidator = new NumericValidator();
        expediaIdValidator.setFieldName("categoryId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        Assert.assertFalse(validationStatus.isValid());
    }
    
    @Test
    public void testValidationOuterDomainValueAndFieldMissing() {
        ImageMessage image = new ImageMessage.ImageMessageBuilder().categoryId("801b").caption("caption").mediaProviderId("1001").build();
        NumericValidator validator = new NumericValidator();
        validator.setFieldName("expediaId");
        ValidationStatus validationStatus = validator.validate(image);
        Assert.assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testValidationOuterDomainValueMissing() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("abc", "hello");
        dataMap.put("def", "hello");
        
        OuterDomainData domainData = new OuterDomainData("LCM", "1e1e", dataMap);
        ImageMessage image = new ImageMessage.ImageMessageBuilder().outerDomainData(domainData).build();
        
        NumericValidator validator = new NumericValidator();
        validator.setFieldName("superID");
        ValidationStatus validationStatus = validator.validate(image);
        Assert.assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testValidationOuterDomainValueFoundValid() {
        final int expedia_test_id = 23419;
        String fieldName = "expediaId";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(fieldName, new Integer(expedia_test_id));
        
        OuterDomainData domainData = new OuterDomainData("LCM", "1e1e", dataMap);
        ImageMessage image = new ImageMessage.ImageMessageBuilder().outerDomainData(domainData).build();
        
        NumericValidator validator = new NumericValidator();
        validator.setFieldName(fieldName);
        ValidationStatus validationStatus = validator.validate(image);
        Assert.assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testValidationOuterDomainValueFoundValidDeepMap() {
        final int expedia_test_id = 23419;
        String fieldName = "expediaId";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("abc", "hello");
        dataMap.put("def", "hello");
        Map<String, Object> dataMap2 = new HashMap<>();
        dataMap.put("ghi", dataMap2);
        dataMap2.put("xyz", "hello");
        dataMap2.put(fieldName, new Integer(expedia_test_id));
        
        OuterDomainData domainData = new OuterDomainData("LCM", "1e1e", dataMap);
        ImageMessage image = new ImageMessage.ImageMessageBuilder().outerDomainData(domainData).build();
        
        NumericValidator validator = new NumericValidator();
        validator.setFieldName(fieldName);
        ValidationStatus validationStatus = validator.validate(image);
        Assert.assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testValidationOuterDomainValueFoundInvalid() {
        String fieldName = "expediaId";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(fieldName, "abcd");

        OuterDomainData domainData = new OuterDomainData("LCM", "1e1e", dataMap);
        ImageMessage image = new ImageMessage.ImageMessageBuilder().outerDomainData(domainData).build();
        
        NumericValidator validator = new NumericValidator();
        validator.setFieldName(fieldName);
        ValidationStatus validationStatus = validator.validate(image);
        Assert.assertFalse(validationStatus.isValid());
        Assert.assertEquals("expediaId is not numeric.", validationStatus.getMessage());
    }
    
}
