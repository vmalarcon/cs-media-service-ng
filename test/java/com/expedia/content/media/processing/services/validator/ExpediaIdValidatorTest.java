package com.expedia.content.media.processing.services.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.domain.ImageType;
import com.expedia.content.media.processing.domain.OuterDomainData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ExpediaIdValidatorTest {
    
    @Test
    public void testValidationExpediaMessagePass() {
        final int expedia_test_id = 23419;
        ImageMessage image =
                new ImageMessage.ImageMessageBuilder().expediaId(expedia_test_id).categoryId("801").caption("caption").mediaProviderId("1001")
                        .imageType(ImageType.LODGING).build();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testValidationMessageWithoutExpediaIdFail() {
        ImageMessage image =
                new ImageMessage.ImageMessageBuilder().categoryId("801").caption("caption").mediaProviderId("1001").imageType(ImageType.LODGING).build();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        assertFalse(validationStatus.isValid());
    }
    
    @Test
    public void testValidationExpediaIdOuterDomainPass() {
        final int expedia_test_id = 23419;
        String fieldName = "expediaId";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(fieldName, new Integer(expedia_test_id));
        OuterDomainData domainData = new OuterDomainData("LCM", dataMap);
        List<OuterDomainData> domainDataList = new ArrayList<>();
        domainDataList.add(domainData);
        
        ImageMessage image =
                new ImageMessage.ImageMessageBuilder().outerDomainDataList(domainDataList).categoryId("801").caption("caption").mediaProviderId("1001")
                        .imageType(ImageType.LODGING).build();
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
        OuterDomainData domainData = new OuterDomainData("LCM", dataMap);
        List<OuterDomainData> domainDataList = new ArrayList<>();
        domainDataList.add(domainData);
        
        ImageMessage image =
                new ImageMessage.ImageMessageBuilder().outerDomainDataList(domainDataList).categoryId("801").caption("caption").mediaProviderId("1001")
                        .imageType(ImageType.VT).build();
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
        OuterDomainData domainData = new OuterDomainData("LCM", dataMap);
        List<OuterDomainData> domainDataList = new ArrayList<>();
        domainDataList.add(domainData);
        
        ImageMessage image = new ImageMessage.ImageMessageBuilder().outerDomainDataList(domainDataList).imageType(ImageType.CARS).build();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        ValidationStatus validationStatus = expediaIdValidator.validate(image);
        assertTrue(validationStatus.isValid());
    }
    
}
