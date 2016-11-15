package com.expedia.content.media.processing.services.util;

import org.springframework.beans.factory.FactoryBean;

/**
 * Allows null object to be injected.
 */
@SuppressWarnings("rawtypes")
public class NullFactoryBean implements FactoryBean {

    private final Class<?> objectType;

    public NullFactoryBean(Class<?> objectType) {
        this.objectType = objectType;
    }

    @Override
    public Object getObject() throws Exception {
        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return objectType;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

}
