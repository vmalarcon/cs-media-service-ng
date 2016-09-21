package com.expedia.content.media.processing.services.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@SuppressWarnings({"PMD.UseUtilityClass"})
public class ReflectionUtil {

    /**
     * sets a static final field in a given class
     * @param field
     * @param newValue
     * @throws Exception
     */
    public static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.set(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }
}
