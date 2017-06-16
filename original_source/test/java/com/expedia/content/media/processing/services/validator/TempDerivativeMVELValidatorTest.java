package com.expedia.content.media.processing.services.validator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.expedia.content.media.processing.services.reqres.TempDerivativeMessage;

@RunWith(SpringJUnit4ClassRunner.class)
// For faster tests, uncomment the following line
@ContextConfiguration(locations = "classpath:mvel-validator.xml")
public class TempDerivativeMVELValidatorTest {
    @BeforeClass
    public static void setUp() {
        System.setProperty("EXPEDIA_ENVIRONMENT", "test");
        System.setProperty("AWS_REGION", "us-west-2");
    }
    
    @Autowired
    TempDerivativeMVELValidator tempDerivativeMVELValidator;
    
    @Test
    public void testLoadContext() {
        assertNotNull(tempDerivativeMVELValidator);
    }
    
    @Test
    public void testMessageFileUrlMissing() throws Exception {
        TempDerivativeMessage tempDerivativeMessage = TempDerivativeMessage.builder().height(null).width(null).fileUrl(null).rotation(null).build();
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("fileUrl is required"));
    }
    
    @Test
    public void testMessageFileUrlMalformed() throws Exception {
        TempDerivativeMessage tempDerivativeMessage =
                TempDerivativeMessage.builder().fileUrl("this is a malformed Url").height(null).width(null).rotation(null).build();
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("fileUrl is malformed"));
    }
    
    @Test
    public void testMessageWidthMissing() throws Exception {
        TempDerivativeMessage tempDerivativeMessage =
                TempDerivativeMessage.builder().fileUrl("s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg").height(180).rotation(null).width(null).build();
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("width is required."));
    }
    
    @Test
    public void testMessageHeightMissing() throws Exception {
        TempDerivativeMessage tempDerivativeMessage =
                TempDerivativeMessage.builder().fileUrl("s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg").height(null).rotation(null).width(180).build();
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("height is required."));
    }
    
    @Test
    public void testMessageRotationMissing() throws Exception {
        TempDerivativeMessage tempDerivativeMessage =
                TempDerivativeMessage.builder().fileUrl("s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg").height(180).rotation(null).width(180).build();
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains(""));
    }
    
    @Test
    public void testMessageRotationNotAcceptedValue() throws Exception {
        TempDerivativeMessage tempDerivativeMessage =
                TempDerivativeMessage.builder().fileUrl("s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg").height(180).rotation("234").width(180).build();
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("rotation accepted values are 0, 90, 180, and 270."));
    }
    
}
