package com.expedia.content.media.processing.services.validator;

import java.util.Map;

/**
 * Abstract class to help validators to seek through the outer domain fields.
 */
public abstract class OuterDomainSeekerValidator {
    
    protected String fieldName;

    /**
     * Searches the outer domain fields for the field to validate. Called recursively to seek
     * through the entire depth of the map. 
     * 
     * @param dataMap The data map to search.
     * @return The value of the field to validate. {@code null} if the value is not found.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Object seekOuterDomainFields(Map<String, Object> dataMap) {
        for (String key : dataMap.keySet()) {
            if (fieldName.equals(key)) {
                return dataMap.get(key);
            } else {
                if (dataMap.get(key) instanceof Map) {
                    Object value = seekOuterDomainFields((Map) dataMap.get(key));
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
