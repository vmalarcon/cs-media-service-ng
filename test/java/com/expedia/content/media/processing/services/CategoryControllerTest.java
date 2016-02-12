package com.expedia.content.media.processing.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.expedia.content.media.processing.services.dao.DomainNotFoundException;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.domain.Category;
import com.expedia.content.media.processing.services.dao.domain.LocalizedName;
import com.expedia.content.media.processing.services.dao.domain.SubCategory;

public class CategoryControllerTest {

    @Test
    public void testSuccess() throws Exception {
        CategoryController categoryController = new CategoryController();

        final String lodgingDoman = "lodging";
        final String localId = "1033";
        MediaDomainCategoriesDao mockMediaDomainCategoriesDao = mock(MediaDomainCategoriesDao.class);
        LocalizedName subcategoryName = new LocalizedName("sub-name", localId);
        List<LocalizedName> subCategoryNames = new ArrayList<>();
        subCategoryNames.add(subcategoryName);
        SubCategory mockSubCategory = new SubCategory("4321", subCategoryNames);
        List<SubCategory> subCategories = new ArrayList<>();
        subCategories.add(mockSubCategory);

        LocalizedName categoryName = new LocalizedName("cat-name", localId);
        List<LocalizedName> categoryNames = new ArrayList<>();
        categoryNames.add(categoryName);
        Category mockCategory = new Category("1234", categoryNames, subCategories);
        List<Category> categories = new ArrayList<>();
        categories.add(mockCategory);
        when(mockMediaDomainCategoriesDao.getMediaCategoriesWithSubCategories(lodgingDoman, localId)).thenReturn(categories);
        setFieldValue(categoryController, "mediaDomainCategoriesDao", mockMediaDomainCategoriesDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> response = categoryController.domainCategories(mockHeader, lodgingDoman, localId);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                /* @formatter:off */
                "{"+
                  "\"domain\":\"lodging\","+
                  "\"categories\":["+
                    "{"+
                      "\"categoryId\":\"1234\","+
                      "\"categoryName\":["+
                        "{"+
                          "\"localizedName\":\"cat-name\","+
                          "\"localeId\":\"1033\""+
                        "}"+
                      "],"+
                      "\"subcategories\":["+
                        "{"+
                          "\"subCategoryId\":\"4321\","+
                          "\"subCategoryName\":["+
                            "{"+
                              "\"localizedName\":\"sub-name\","+
                              "\"localeId\":\"1033\""+
                            "}"+
                          "]"+
                        "}"+
                      "]"+
                    "}"+
                  "]"+
                "}",
                /* @formatter:on */
                response.getBody());
    }
    
    @Test
    public void testDomainNotFound() throws Exception {
        CategoryController categoryController = new CategoryController();

        final String lodgingDoman = "potato";
        final String localId = "1033";
        MediaDomainCategoriesDao mockMediaDomainCategoriesDao = mock(MediaDomainCategoriesDao.class);
        when(mockMediaDomainCategoriesDao.getMediaCategoriesWithSubCategories(lodgingDoman, localId)).thenThrow(new DomainNotFoundException("test exception"));
        setFieldValue(categoryController, "mediaDomainCategoriesDao", mockMediaDomainCategoriesDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> response = categoryController.domainCategories(mockHeader, lodgingDoman, localId);
        assertEquals(404, response.getStatusCode().value());
        assertTrue(response.getBody().startsWith("{\"error\":\"Not Found\",\"message\":\"Requested resource with ID potato was not found.\",\"path\":\"/media/v1/domaincategories/potato?localeId=1033\",\"status\":404,\"timestamp\"", 0));
    }

    private static void setFieldValue(Object obj, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        final Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

}
