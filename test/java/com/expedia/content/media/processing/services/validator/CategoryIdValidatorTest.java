package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.domain.ImageType;
import com.expedia.content.media.processing.domain.StagingKeyMap;
import junit.framework.Assert;
import org.junit.Test;

import java.net.URL;

/**
 * Created by seli on 2015-07-14.
 */
public class CategoryIdValidatorTest {

    @Test
    public void testValidationCategoryMessagePass(){

        ImageMessage image =new ImageMessage(null,null,"","","",null,new Integer(2),"123","","",null);
        CategoryIdValidator categoryIdValidator =new CategoryIdValidator();
        categoryIdValidator.setFieldName("categoryId");
        ValidationStatus validationStatus= categoryIdValidator.validate(image);
        org.junit.Assert.assertTrue(validationStatus.isStatus());
    }

    @Test
    public void testValidationCategoryMessageFail(){

        ImageMessage image =new ImageMessage(null,null,"","","",null,new Integer(2),"123aa","","",null);
        CategoryIdValidator categoryIdValidator =new CategoryIdValidator();
        categoryIdValidator.setFieldName("categoryId");
        ValidationStatus validationStatus= categoryIdValidator.validate(image);
        org.junit.Assert.assertFalse(validationStatus.isStatus());
    }
}
