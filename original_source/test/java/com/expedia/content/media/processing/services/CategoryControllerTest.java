package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.dao.DomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.domain.Category;
import com.expedia.content.media.processing.services.dao.domain.LocalizedName;
import com.expedia.content.media.processing.services.dao.domain.Subcategory;

import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CategoryControllerTest {

    private final String LOCALE_ID = "1033";

    @Mock
    private DomainCategoriesDao mockMediaDomainCategoriesDao;
    @Mock
    private Poker poker;

    private CategoryController categoryController;
    
    @Before
    public void initialize() throws IllegalAccessException, NoSuchFieldException {
        categoryController = new CategoryController(mockMediaDomainCategoriesDao, poker);
    }

    @Test
    public void testSuccess() throws Exception {
        final String lodgingDomain = "lodging";
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        List<Category> categoryList = new ArrayList<>();
        LocalizedName primaryImage = new LocalizedName("Primary Image", LOCALE_ID);
        LocalizedName featuredImage = new LocalizedName("Featured Image", LOCALE_ID);
        LocalizedName catName = new LocalizedName("cat-name", LOCALE_ID);
        LocalizedName subName = new LocalizedName("sub-name", LOCALE_ID);
        LocalizedName empty = new LocalizedName("", LOCALE_ID);
        Subcategory subcategory1 = new Subcategory("3", Arrays.asList(featuredImage));
        Subcategory subcategory2 = new Subcategory("4321", Arrays.asList(subName));
        Subcategory subcategory3 = new Subcategory("0", Arrays.asList(empty));
        Category category1 = new Category("3", Arrays.asList(primaryImage), Arrays.asList(subcategory1));
        Category category2 = new Category("1234", Arrays.asList(catName), Arrays.asList(subcategory2));
        Category category3 = new Category("1", Arrays.asList(empty), Arrays.asList(subcategory3));
        categoryList.add(category1);
        categoryList.add(category2);
        categoryList.add(category3);
        when(mockMediaDomainCategoriesDao.getMediaCategoriesWithSubCategories(lodgingDomain, LOCALE_ID)).thenReturn(categoryList);
        ResponseEntity<String> response = categoryController.domainCategories(mockHeader, lodgingDomain, LOCALE_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Make sure featured (3) categories and null (0) subcategories are not returned.
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
                          "\"subcategoryId\":\"4321\","+
                          "\"subcategoryName\":["+
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
        final String lodgingDomain = "potato";
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        ResponseEntity<String> response = categoryController.domainCategories(mockHeader, lodgingDomain, LOCALE_ID);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().startsWith("{\"error\":\"Not Found\",\"message\":\"Requested resource with ID potato was not found.\",\"path\":\"/media/v1/domaincategories/potato?localeId=1033\",\"status\":404,\"timestamp\"", 0));
    }

    @Test
    public void testInvalidLocalId() throws Exception {
        final String lodgingDomain = "potato";
        final String localId = "10335555";
        String requestId = "test-request-id";
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("request-id", requestId);
        ResponseEntity<String> responseEntity = categoryController.domainCategories(headers, lodgingDomain, localId);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody().contains("\"message\":\"Requested localeId " + localId + " must be a number less than 5 characters.\""));
    }

    @Test(expected = IllegalStateException.class)
    public void pokeTest() throws Exception {
        final String lodgingDomain = "lodging";
        IllegalStateException exception = new IllegalStateException("this is an IllegalStateException exception");
        setFieldValue(categoryController, "hipChatRoom", "EWE CS: Phoenix Notifications");
        Poker poker = mock(Poker.class);
        setFieldValue(categoryController, "poker", poker);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        when(mockMediaDomainCategoriesDao.getMediaCategoriesWithSubCategories(lodgingDomain, LOCALE_ID)).thenThrow(exception);
        categoryController.domainCategories(mockHeader, lodgingDomain, LOCALE_ID);
        verify(poker).poke("Media Services failed to process a domainCategories request - RequestId: test-request-id", "EWE CS: Phoenix Notifications",
                MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl() + "/" + lodgingDomain + LOCALE_ID, eq(exception));
    }
}
