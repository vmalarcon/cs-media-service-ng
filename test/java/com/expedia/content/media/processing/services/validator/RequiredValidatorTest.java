package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RequiredValidatorTest {
    
    @Test
    public void testDomainFieldValuePresent() throws Exception {
        OuterDomain domainData = new OuterDomain(Domain.LODGING, null, null, null, null);
        ImageMessage image = new ImageMessage.ImageMessageBuilder().outerDomainData(domainData).fileUrl(("http://www.asdf.com")).build();
        RequiredValidator requiredValidator = new RequiredValidator();
        requiredValidator.setFieldName("fileUrl");
        ValidationStatus validationStatus = requiredValidator.validate(image);
        assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testDomainFieldValueNotPresent() {
        OuterDomain domainData = new OuterDomain(Domain.LODGING, null, null, null, null);
        ImageMessage image = new ImageMessage.ImageMessageBuilder().outerDomainData(domainData).build();
        RequiredValidator requiredValidator = new RequiredValidator();
        requiredValidator.setFieldName("fileUrl");
        ValidationStatus validationStatus = requiredValidator.validate(image);
        assertFalse(validationStatus.isValid());
    }
    
    @Test
    public void testOuterDomainFieldValuePresent() throws Exception {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("abc", "hello");
        dataMap.put("def", "hello");
        OuterDomain domainData = new OuterDomain(Domain.LODGING, null, "1001", "1e1e", dataMap);
        ImageMessage image = new ImageMessage.ImageMessageBuilder().outerDomainData(domainData).build();
        RequiredValidator requiredValidator = new RequiredValidator();
        requiredValidator.setFieldName("abc");
        ValidationStatus validationStatus = requiredValidator.validate(image);
        assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testOuterDomainFieldValueNotPresent() {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("abc", "hello");
        dataMap.put("def", "hello");
        OuterDomain domainData = new OuterDomain(Domain.LODGING, null, "1001", "1e1e", dataMap);
        ImageMessage image = new ImageMessage.ImageMessageBuilder().outerDomainData(domainData).build();
        RequiredValidator requiredValidator = new RequiredValidator();
        requiredValidator.setFieldName("xyz");
        ValidationStatus validationStatus = requiredValidator.validate(image);
        assertFalse(validationStatus.isValid());
    }

}
