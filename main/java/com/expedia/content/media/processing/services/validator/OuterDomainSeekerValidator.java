package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;

import java.util.Map;

/**
 * Abstract class to help validators to seek through the outer domain fields.
 *
 * @deprecated Use MVELValidator instead
 */
@Deprecated
@SuppressWarnings({"PMD.AbstractClassWithoutAbstractMethod"})
public abstract class OuterDomainSeekerValidator {

    protected String fieldName;

    /**
     * Searches the outer domain fields for the field to validate. Called recursively to seek
     * through the entire depth of the map.
     * 
     * @param message The data map to search.
     * @return The value of the field to validate. {@code null} if the value is not found.
     */
    protected Object seekOuterDomainFields(ImageMessage message) {
        if (message.getOuterDomainData() != null) {
            final Map<String, Object> dataMap = message.getOuterDomainData().getDomainFields();
            return scanOuterDomainDataTree(dataMap);

        }
        return null;
    }

    /**
     * @param dataMap
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object scanOuterDomainDataTree(Map<String, Object> dataMap) {
        if (dataMap == null) {
            return null;
        }
        for (final String key : dataMap.keySet()) {
            if (fieldName.equals(key)) {
                return dataMap.get(key);
            } else {
                if (dataMap.get(key) instanceof Map) {
                    final Object value = scanOuterDomainDataTree((Map) dataMap.get(key));
                    if (value != null) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

}
