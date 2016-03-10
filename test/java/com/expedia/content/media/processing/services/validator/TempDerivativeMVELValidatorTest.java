package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.services.reqres.TempDerivativeMessage;
import com.expedia.content.media.processing.services.TempDerivativeController;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        TempDerivativeMessage tempDerivativeMessage = new TempDerivativeMessage(null, null, null, null);
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("fileUrl is required"));
    }

    @Test
    public void testMessageFileUrlMalformed() throws Exception {
        TempDerivativeMessage tempDerivativeMessage = new TempDerivativeMessage("this is a malformed Url", null, null, null);
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("fileUrl is malformed"));
    }

    @Test
    public void testMessageWidthMissing() throws Exception {
        String jsonMsg = "{ " +
                "\"fileUrl\": \"s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg\"," +
                "\"rotation\": \"180\"," +
                "\"height\": 180" +
                " }";
        TempDerivativeMessage tempDerivativeMessage = new TempDerivativeMessage("s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg", null, null, 180);
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("width is required."));
    }

    @Test
    public void testMessageHeightMissing() throws Exception {
        String jsonMsg = "{ " +
                "\"fileUrl\": \"s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg\"," +
                "\"rotation\": \"180\"," +
                "\"width\": 180" +
                " }";
        TempDerivativeMessage tempDerivativeMessage = new TempDerivativeMessage("s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg", null, 180, null);
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("height is required."));
    }

    @Test
    public void testMessageRotationMissing() throws Exception {
        TempDerivativeMessage tempDerivativeMessage = new TempDerivativeMessage("s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg", null, 180, 180);
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains(""));
    }

    @Test
    public void testMessageRotationNotAcceptedValue() throws Exception {
        String jsonMsg = "{ " +
                "\"fileUrl\": \"s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg\"," +
                "\"rotation\": \"234\"," +
                "\"width\": 180," +
                "\"height\": 180" +
                " }";
        TempDerivativeMessage tempDerivativeMessage = new TempDerivativeMessage("s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg", "234", 180, 180);
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("rotation accepted values are 0, 90, 180, and 270."));
    }

}
