package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.reporting.FormattedLogger;

import java.lang.reflect.Field;

/**
 * use java reflection to get field value.
 */
public final class ReflectionUtil {

    private static final FormattedLogger LOGGER = new FormattedLogger(NumericValidator.class);

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
            final Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            if (field.getName().equals(fieldName)) {
                objectValue = field.get(obj);
            }
        } catch (NoSuchFieldException e) {
            LOGGER.error(e, "getFieldValue failed ErrorMessage={}", e.getMessage());
            throw e;
        }
        LOGGER.debug("getFiledValue Field={} ReturnedValue={}", fieldName, objectValue);
        return objectValue;
    }
}
