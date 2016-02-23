package com.expedia.content.media.processing.services.testing;

import java.lang.reflect.Field;

public class TestingUtil {
    
    private TestingUtil() {}
    
    public static void setFieldValue(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        final Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

}
