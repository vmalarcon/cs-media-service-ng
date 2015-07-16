package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Base implementation of ImageMessage numeric field validation
 */
public class NumericValidator implements MediaMessageValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NumericValidator.class);
    protected String fieldName;

    /**
     * this method will validate specific field is a number
     *
     * @param imageMessage message to validate
     * @return ValidationStatus contain two validation status, true-successful,
     * false- validation fail , in false case, a validation message is set in ValidationStatus
     */
    @Override
    public ValidationStatus validate(ImageMessage imageMessage) {
        ValidationStatus validationStatus = new ValidationStatus();

        if (!validateFieldNumeric(imageMessage)) {
            validationStatus.setValid(false);
            validationStatus.setMessage(fieldName + " is not numeric");
        } else {
            validationStatus.setValid(true);
        }
        return validationStatus;
    }

    /**
     * validate the field value is a number or not
     *
     * @param imageMessage
     * @return boolean
     */
    private boolean validateFieldNumeric(ImageMessage imageMessage) {
        Object fieldValue = getFieldValue(imageMessage);
        if (fieldValue != null && !StringUtils.isNumeric(fieldValue.toString())) {
            return false;
        }
        return true;
    }

    /**
     * this method use reflection to get the object field value.
     *
     * @param obj
     * @return field value
     */
    private Object getFieldValue(Object obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        Object objectValue = null;
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                if (field.getName().equals(fieldName)) {
                    objectValue = field.get(obj);
                }
            } catch (Exception e) {
                LOGGER.error("getFieldValue fail", e);
            }
        }
        LOGGER.debug("getFiledValue for field {} return value:{}", fieldName, objectValue);
        return objectValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
