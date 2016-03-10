package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.derivative.TempDerivativeMessage;
import com.expedia.content.media.processing.services.util.JSONUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by sstannus on 3/10/16.
 */
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
        String jsonMsg = "{}";
        TempDerivativeMessage tempDerivativeMessage = JSONUtil.buildTempDerivativeFromJSONMessage(jsonMsg);
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("fileUrl is required"));
    }

    @Test
    public void testMessageFileUrlMalformed() throws Exception {
        String jsonMsg = "{ \"fileUrl\": \"this is a malformed Url\" }";
        TempDerivativeMessage tempDerivativeMessage = JSONUtil.buildTempDerivativeFromJSONMessage(jsonMsg);
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
        TempDerivativeMessage tempDerivativeMessage = JSONUtil.buildTempDerivativeFromJSONMessage(jsonMsg);
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
        TempDerivativeMessage tempDerivativeMessage = JSONUtil.buildTempDerivativeFromJSONMessage(jsonMsg);
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("height is required."));
    }

    @Test
    public void testMessageRotationMissing() throws Exception {
        String jsonMsg = "{ " +
                "\"fileUrl\": \"s3://ewe-cs-media-test/e2e/images/9oZkgVs.jpg\"," +
                "\"width\": 180," +
                "\"height\": 180" +
                " }";
        TempDerivativeMessage tempDerivativeMessage = JSONUtil.buildTempDerivativeFromJSONMessage(jsonMsg);
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
        TempDerivativeMessage tempDerivativeMessage = JSONUtil.buildTempDerivativeFromJSONMessage(jsonMsg);
        String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
        assertTrue(errors.contains("rotation value is not an accepted value."));
    }

}
