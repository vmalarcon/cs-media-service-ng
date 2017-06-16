package com.expedia.content.media.processing.services.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidatorUtilTest {

    @Test
    public void testNullUuid() {
        assertFalse(ValidatorUtil.isValidUUID(null));
    }

    @Test
    public void testNullStringUuid() {
        assertFalse(ValidatorUtil.isValidUUID("null"));
    }

    @Test
    public void testInvalidFormatUuid() {
        assertFalse(ValidatorUtil.isValidUUID("invalid-format-uuid"));
    }

    @Test
    public void testValidFormatUuid() {
        assertTrue(ValidatorUtil.isValidUUID("12345678-9abc-def0-1234-56789abcdef0"));
    }

}
