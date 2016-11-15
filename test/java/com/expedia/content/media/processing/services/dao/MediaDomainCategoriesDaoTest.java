package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.domain.Category;
import com.expedia.content.media.processing.services.dao.domain.MediaCategory;
import com.expedia.content.media.processing.services.dao.domain.MediaSubCategory;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaDomainCategoriesSproc;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MediaDomainCategoriesDaoTest {

    @Mock
    private SQLMediaDomainCategoriesSproc mockSQLMediaDomainCategoriesSproc;

    @Mock
    private Map<String, Object> mockResults;

    Map<String, Object> domainField = new HashMap<>();

    @Test
    public void testDomainFound() throws Exception {
        String domain = "lodging";
        String localeId = "1033";
        List<MediaCategory> mockMediaCategories = new ArrayList<>();
        mockMediaCategories.add(new MediaCategory("3", "1033", "Primary Image"));
        mockMediaCategories.add(new MediaCategory("4", "1033", "Lobby"));
        mockMediaCategories.add(new MediaCategory("5", "1033", "Guestroom"));
        List<MediaSubCategory> mockMediaSubCategories = new ArrayList<>();
        mockMediaSubCategories.add(new MediaSubCategory("3", "3", "1033", "Featured Image"));
        mockMediaSubCategories.add(new MediaSubCategory("4", "10000", "1033", "Interior Entrance"));
        mockMediaSubCategories.add(new MediaSubCategory("4", "10001", "1033", "Lobby"));
        mockMediaSubCategories.add(new MediaSubCategory("5", "22022", "1033", "Minibar"));
        MediaDomainCategoriesDao mediaDomainCategoriesDao = new MediaDomainCategoriesDao(mockSQLMediaDomainCategoriesSproc);
        when(mockSQLMediaDomainCategoriesSproc.execute(localeId)).thenReturn(mockResults);
        when(mockResults.get(SQLMediaDomainCategoriesSproc.MEDIA_CATEGORY_RESULT_SET)).thenReturn(mockMediaCategories);
        when(mockResults.get(SQLMediaDomainCategoriesSproc.MEDIA_SUB_CATEGORY_RESULT_SET)).thenReturn(mockMediaSubCategories);
        List<Category> categories = mediaDomainCategoriesDao.getMediaCategoriesWithSubCategories(domain, localeId);
        assertNotEquals(categories, null);
        assertEquals(2, categories.size());
        assertEquals(0, categories.stream().filter(category -> category.getCategoryId().equals("3")).collect(Collectors.toList()).size());
    }

    @Test
    public void testDomainCategoryWithoutSubCategories() throws Exception {
        String domain = "lodging";
        String localeId = "1033";
        List<MediaCategory> mockMediaCategories = new ArrayList<>();
        mockMediaCategories.add(new MediaCategory("4", "1033", "Lobby"));
        mockMediaCategories.add(new MediaCategory("5", "1033", "Guestroom"));
        List<MediaSubCategory> mockMediaSubCategories = new ArrayList<>();
        MediaDomainCategoriesDao mediaDomainCategoriesDao = new MediaDomainCategoriesDao(mockSQLMediaDomainCategoriesSproc);
        when(mockSQLMediaDomainCategoriesSproc.execute(localeId)).thenReturn(mockResults);
        when(mockResults.get(SQLMediaDomainCategoriesSproc.MEDIA_CATEGORY_RESULT_SET)).thenReturn(mockMediaCategories);
        when(mockResults.get(SQLMediaDomainCategoriesSproc.MEDIA_SUB_CATEGORY_RESULT_SET)).thenReturn(mockMediaSubCategories);
        List<Category> categories = mediaDomainCategoriesDao.getMediaCategoriesWithSubCategories(domain, localeId);
        assertNotEquals(categories, null);
        assertEquals(2, categories.size());
        assertEquals(categories.stream().filter(category -> category.getCategoryId().equals("3")).collect(Collectors.toList()).size(), 0);
    }

    /**
     * This case will actually never happen in prod
     * @throws Exception
     */
    @Test
    public void testDomainSubCategoriesWithoutParentCategories() throws Exception {
        String domain = "lodging";
        String localeId = "1033";
        List<MediaCategory> mockMediaCategories = new ArrayList<>();
        List<MediaSubCategory> mockMediaSubCategories = new ArrayList<>();
        mockMediaSubCategories.add(new MediaSubCategory("4", "10000", "1033", "Interior Entrance"));
        mockMediaSubCategories.add(new MediaSubCategory("4", "10001", "1033", "Lobby"));
        mockMediaSubCategories.add(new MediaSubCategory("5", "22022", "1033", "Minibar"));
        MediaDomainCategoriesDao mediaDomainCategoriesDao = new MediaDomainCategoriesDao(mockSQLMediaDomainCategoriesSproc);
        when(mockSQLMediaDomainCategoriesSproc.execute(localeId)).thenReturn(mockResults);
        when(mockResults.get(SQLMediaDomainCategoriesSproc.MEDIA_CATEGORY_RESULT_SET)).thenReturn(mockMediaCategories);
        when(mockResults.get(SQLMediaDomainCategoriesSproc.MEDIA_SUB_CATEGORY_RESULT_SET)).thenReturn(mockMediaSubCategories);
        List<Category> categories = mediaDomainCategoriesDao.getMediaCategoriesWithSubCategories(domain, localeId);
        assertEquals(categories.size(), 0);
    }

    @Test(expected = DomainNotFoundException.class)
    public void testDomainNotFound() throws Exception {
        String domain = "cats";
        String localeId = "1033";
        MediaDomainCategoriesDao mediaDomainCategoriesDao = new MediaDomainCategoriesDao(mockSQLMediaDomainCategoriesSproc);
        mediaDomainCategoriesDao.getMediaCategoriesWithSubCategories(domain, localeId);
    }

    @Test
    public  void testCategoryIdExists() {
        domainField.put("subcategoryId", "22022");
        domainField.put("propertyHero", "true");
        String localeId = "1033";
        List<MediaCategory> mockMediaCategories = new ArrayList<>();
        mockMediaCategories.add(new MediaCategory("4", "1033", "Lobby"));
        mockMediaCategories.add(new MediaCategory("5", "1033", "Guestroom"));
        List<MediaSubCategory> mockMediaSubCategories = new ArrayList<>();
        mockMediaSubCategories.add(new MediaSubCategory("4", "10000", "1033", "Interior Entrance"));
        mockMediaSubCategories.add(new MediaSubCategory("4", "10001", "1033", "Lobby"));
        mockMediaSubCategories.add(new MediaSubCategory("5", "22022", "1033", "Minibar"));
        MediaDomainCategoriesDao mediaDomainCategoriesDao = new MediaDomainCategoriesDao(mockSQLMediaDomainCategoriesSproc);
        when(mockSQLMediaDomainCategoriesSproc.execute(localeId)).thenReturn(mockResults);
        when(mockResults.get(SQLMediaDomainCategoriesSproc.MEDIA_CATEGORY_RESULT_SET)).thenReturn(mockMediaCategories);
        when(mockResults.get(SQLMediaDomainCategoriesSproc.MEDIA_SUB_CATEGORY_RESULT_SET)).thenReturn(mockMediaSubCategories);
        Boolean categoryExists = mediaDomainCategoriesDao.subCategoryIdExists(new OuterDomain(Domain.LODGING, "123", "", "EPC Internal User", domainField), localeId);
        assertTrue(categoryExists);
        verify(mockSQLMediaDomainCategoriesSproc, times(1)).execute("1033");
    }

    @Test
    public  void testCategoryDoesNotExist() {
        domainField.put("subcategoryId", "22005");
        domainField.put("propertyHero", "true");
        String localeId = "1033";
        List<MediaCategory> mockMediaCategories = new ArrayList<>();
        mockMediaCategories.add(new MediaCategory("4", "1033", "Lobby"));
        mockMediaCategories.add(new MediaCategory("5", "1033", "Guestroom"));
        List<MediaSubCategory> mockMediaSubCategories = new ArrayList<>();
        mockMediaSubCategories.add(new MediaSubCategory("4", "10000", "1033", "Interior Entrance"));
        mockMediaSubCategories.add(new MediaSubCategory("4", "10001", "1033", "Lobby"));
        mockMediaSubCategories.add(new MediaSubCategory("5", "22022", "1033", "Minibar"));
        MediaDomainCategoriesDao mediaDomainCategoriesDao = new MediaDomainCategoriesDao(mockSQLMediaDomainCategoriesSproc);
        when(mockSQLMediaDomainCategoriesSproc.execute(localeId)).thenReturn(mockResults);
        when(mockResults.get(SQLMediaDomainCategoriesSproc.MEDIA_CATEGORY_RESULT_SET)).thenReturn(mockMediaCategories);
        when(mockResults.get(SQLMediaDomainCategoriesSproc.MEDIA_SUB_CATEGORY_RESULT_SET)).thenReturn(mockMediaSubCategories);
        Boolean categoryExists = mediaDomainCategoriesDao.subCategoryIdExists(new OuterDomain(Domain.LODGING, "123", "", "EPC Internal User", domainField), localeId);
        assertFalse(categoryExists);
        verify(mockSQLMediaDomainCategoriesSproc, times(1)).execute("1033");
    }
}
