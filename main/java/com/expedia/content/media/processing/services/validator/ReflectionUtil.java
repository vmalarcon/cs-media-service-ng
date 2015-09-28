package com.expedia.content.media.processing.services.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * use java reflection to get field value.
 */
public final class ReflectionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(NumericValidator.class);

    private ReflectionUtil() {
    }

    /**
     * this method use reflection to get the object field value.
     *
     * @param obj
     * @param fieldName
     * @return field value
     */
    public static Object getFieldValue(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Object objectValue = null;
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            if (field.getName().equals(fieldName)) {
                objectValue = field.get(obj);
            }
        } catch (NoSuchFieldException e) {
            LOGGER.error("getFieldValue failed : error=[{}]", e);
            throw e;
        }
        LOGGER.debug("getFiledValue for field={}, return value=[{}]", fieldName, objectValue);
        return objectValue;
    }
}
