package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.dao.DomainNotFoundException;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.domain.MediaCategory;
import com.expedia.content.media.processing.services.dao.domain.MediaSubCategory;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaDomainCategoriesSproc;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CategoryControllerTest {

    @Mock
    SQLMediaDomainCategoriesSproc mockSQLMediaDomainCategoriesSproc;


    Map<String, Object> mediaProviderMockResults;
    Map<String, Object> catMockResults;
    final String LOCALID = "1033";
    List<MediaCategory> mockMediaCategories;
    List<MediaSubCategory> mockMediaSubCategories;
    MediaDomainCategoriesDao mockMediaDomainCategoriesDao;
    
    @Before
    public void setup(){
        mediaProviderMockResults = new HashMap<>();
        catMockResults = new HashMap<>();
        mockMediaCategories = new ArrayList<>();
        mockMediaCategories.add(new MediaCategory("3", LOCALID, "Primary Image"));
        mockMediaCategories.add(new MediaCategory("1234", LOCALID, "cat-name"));
        mockMediaCategories.add(new MediaCategory("1", LOCALID, ""));
        mockMediaSubCategories = new ArrayList<>();
        mockMediaSubCategories.add(new MediaSubCategory("3", "3", LOCALID, "Featured Image"));
        mockMediaSubCategories.add(new MediaSubCategory("1234", "4321", LOCALID, "sub-name"));
        mockMediaSubCategories.add(new MediaSubCategory("1", "0", LOCALID, ""));
        catMockResults.put(SQLMediaDomainCategoriesSproc.MEDIA_SUB_CATEGORY_RESULT_SET, mockMediaSubCategories);
        catMockResults.put(SQLMediaDomainCategoriesSproc.MEDIA_CATEGORY_RESULT_SET, mockMediaCategories);
        mockMediaDomainCategoriesDao = spy(new MediaDomainCategoriesDao(mockSQLMediaDomainCategoriesSproc));
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
    }

    @Test
    public void testSuccess() throws Exception {
        CategoryController categoryController = new CategoryController();

        final String lodgingDoman = "lodging";
        setFieldValue(categoryController, "mediaDomainCategoriesDao", mockMediaDomainCategoriesDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        ResponseEntity<String> response = categoryController.domainCategories(mockHeader, lodgingDoman, LOCALID);
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
        CategoryController categoryController = new CategoryController();

        final String lodgingDoman = "potato";
        final String localId = LOCALID;
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

    @Test
    public void testInvalidLocalId() throws Exception {
        final String lodgingDoman = "potato";
        final String localId = "10335555";
        CategoryController categoryController = new CategoryController();
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("request-id", "testid");
        ResponseEntity<String> responseEntity = categoryController.domainCategories(headers, lodgingDoman, localId);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test(expected = IllegalStateException.class)
    public void pokeTest() throws Exception {
        CategoryController categoryController = new CategoryController();

        final String lodgingDoman = "lodging";
        setFieldValue(categoryController, "mediaDomainCategoriesDao", mockMediaDomainCategoriesDao);
        IllegalStateException exception = new IllegalStateException("this is an IllegalStateException exception");
        setFieldValue(categoryController, "hipChatRoom", "EWE CS: Phoenix Notifications");
        Poker poker = mock(Poker.class);
        setFieldValue(categoryController, "poker", poker);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        when(categoryController.domainCategories(mockHeader, lodgingDoman, LOCALID)).thenThrow(exception);

        categoryController.domainCategories(mockHeader, lodgingDoman, LOCALID);
        verify(poker).poke("Media Services failed to process a domainCategories request - RequestId: test-request-id", "EWE CS: Phoenix Notifications",
                MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl() + "/" + lodgingDoman + LOCALID, eq(exception));
    }
}
