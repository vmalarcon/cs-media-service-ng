package com.expedia.content.media.processing.services.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.domain.ImageType;
import com.expedia.content.media.processing.domain.OuterDomainData;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class RequiredValidatorTest {
    
    @Test
    public void testDomainFieldValuePresent() throws Exception {
        ImageMessage image = new ImageMessage.ImageMessageBuilder().imageType(ImageType.LODGING).fileUrl(new URL("http://www.asdf.com")).build();
        RequiredValidator requiredValidator = new RequiredValidator();
        requiredValidator.setFieldName("fileUrl");
        ValidationStatus validationStatus = requiredValidator.validate(image);
        assertTrue(validationStatus.isValid());
    }
    
    @Test
    public void testDomainFieldValueNotPresent() {
        ImageMessage image = new ImageMessage.ImageMessageBuilder().imageType(ImageType.LODGING).build();
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
        OuterDomainData domainData = new OuterDomainData("LCM", "1e1e", dataMap);
        List<OuterDomainData> domainDataList = new ArrayList<>();
        domainDataList.add(domainData);

        ImageMessage image = new ImageMessage.ImageMessageBuilder().imageType(ImageType.LODGING).outerDomainDataList(domainDataList).build();
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
        OuterDomainData domainData = new OuterDomainData("LCM", "1e1e", dataMap);
        List<OuterDomainData> domainDataList = new ArrayList<>();
        domainDataList.add(domainData);

        ImageMessage image = new ImageMessage.ImageMessageBuilder().imageType(ImageType.LODGING).outerDomainDataList(domainDataList).build();
        RequiredValidator requiredValidator = new RequiredValidator();
        requiredValidator.setFieldName("xyz");
        ValidationStatus validationStatus = requiredValidator.validate(image);
        assertFalse(validationStatus.isValid());
    }
    
}
