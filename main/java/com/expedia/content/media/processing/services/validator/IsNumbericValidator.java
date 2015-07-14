package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Created by seli on 2015-07-14.
 * this class is parent class that can be used to check field is number or not
 */
public class IsNumbericValidator implements MediaMessageValidator {
    protected String fieldName;
    private static final Logger LOGGER = LoggerFactory.getLogger(IsNumbericValidator.class);

    @Override public ValidationStatus validate(ImageMessage imageMessage) {
        ValidationStatus validationStatus = new ValidationStatus();

        if (validateFieldNumberic(imageMessage) == false) {
            validationStatus.setStatus(false);
            validationStatus.setMessage(fieldName + " is not numberic");
        } else {
            validationStatus.setStatus(true);
        }
        return validationStatus;
    }

    public boolean validateFieldNumberic(ImageMessage imageMessage) {
        Object fieldValue = getFieldValue(imageMessage);
        if (fieldValue != null && !StringUtils.isNumeric(fieldValue.toString())) {
            return false;
        }
        return true;
    }

    /*
    *this method use reflection to get the object field value.
     */
    private Object getFieldValue(Object obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        Object objectValue = null;
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                if (field.getName().equals(fieldName))
                    objectValue = field.get(obj);

            } catch (Exception e) {
                LOGGER.error("getFieldValue fail",e);
            }
        }
        LOGGER.debug("getFiledValue for field {} return value:{}",fieldName,objectValue);
        return objectValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
