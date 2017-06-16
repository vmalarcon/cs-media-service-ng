package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.dao.domain.Category;
import com.expedia.content.media.processing.services.dao.domain.DomainCategory;
import com.expedia.content.media.processing.services.dao.domain.DomainIdMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.util.MediaDBSQLUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.expedia.content.media.processing.services.util.MediaDBSQLUtil.buildDomainIdMediaFromResultSet;
import static com.expedia.content.media.processing.services.util.MediaDBSQLUtil.buildMediaFromResultSet;
import static com.expedia.content.media.processing.services.util.MediaDBSQLUtil.domainCategoryListToCategoryList;
import static com.expedia.content.media.processing.services.util.MediaDBSQLUtil.setArray;
import static com.expedia.content.media.processing.services.util.MediaDBSQLUtil.setMediaByDomainIdQueryString;
import static com.expedia.content.media.processing.services.util.MediaDBSQLUtil.setSQLTokensWithArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MediaDBSQLUtilTest {
    @Mock
    private PreparedStatement mockStatement;
    @Mock
    private ResultSet mockResultSet;


    @Test
    public void setArrayTest() throws Exception {
        String[] array = new String[]{"hamburger", "apples", "cheese"};
        Integer endIndex = setArray(mockStatement, 1, array);
        assertTrue(4 == endIndex);
        verify(mockStatement, times(3)).setString(anyInt(), anyString());
    }

    @Test
    public void setArrayTestError() throws Exception {
        String[] array = new String[]{"hamburger", "apples", "cheese"};
        doThrow(SQLDataException.class).when(mockStatement).setString(anyInt(), anyString());
        Integer endIndex = setArray(mockStatement, 1, array);
        assertTrue(1 == endIndex);
        verify(mockStatement, times(3)).setString(anyInt(), anyString());
    }

    @Test
    public void setSQLTokensWithArrayTest() throws Exception {
        String sqlString = "SELECT * FROM Media WHERE media-id IN (?)";
        String[] array = new String[]{"hamburger", "apples", "cheese"};
        String result = setSQLTokensWithArray(sqlString, array);
        assertEquals("SELECT * FROM Media WHERE media-id IN (?,?,?)", result);
    }

    @Test
    public void setSQLTokensWithArrayTestWithOnlyOneElement() throws Exception {
        String sqlString = "SELECT * FROM Media WHERE media-id IN (?)";
        String[] array = new String[]{"hamburger"};
        String result = setSQLTokensWithArray(sqlString, array);
        assertEquals("SELECT * FROM Media WHERE media-id IN (?)", result);
    }

    @Test
    public void convertLcmMediaIdInMapToString() {
        Map<String, Object> domainFieldsMap = new HashMap<>();
        domainFieldsMap.put("lcmMediaId", 123);
        Map<String, Object> domainFieldsMapResult = MediaDBSQLUtil.convertLcmMediaIdInMapToString(domainFieldsMap);
        assertEquals("123", domainFieldsMapResult.get("lcmMediaId"));
    }

    @Test
    public void convertLcmMediaIdInMapToStringWhenAlreadyString() {
        Map<String, Object> domainFieldsMap = new HashMap<>();
        domainFieldsMap.put("lcmMediaId", "123");
        Map<String, Object> domainFieldsMapResult = MediaDBSQLUtil.convertLcmMediaIdInMapToString(domainFieldsMap);
        assertEquals("123", domainFieldsMapResult.get("lcmMediaId"));
    }

    @Test
    public void convertLcmMediaIdInMapToStringWithNoLcmMediaId() {
        Map<String, Object> domainFieldsMap = new HashMap<>();
        domainFieldsMap.put("subcategoryId", "123");
        Map<String, Object> domainFieldsMapResult = MediaDBSQLUtil.convertLcmMediaIdInMapToString(domainFieldsMap);
        assertEquals(domainFieldsMap, domainFieldsMapResult);
    }

    @Test
    public void domainCategoryListToCategoryListTest() {
        List<DomainCategory> domainCategoryList = new ArrayList<>();
        DomainCategory domainCategory0 = DomainCategory.builder()
                .categoryId("3")
                .localeId("1033")
                .localizedName("parking")
                .build();
        DomainCategory domainCategory1 = DomainCategory.builder()
                .parentCategoryId("0")
                .categoryId("10001")
                .localeId("1033")
                .localizedName("something cool")
                .build();
        DomainCategory domainCategory2 = DomainCategory.builder()
                .parentCategoryId("0")
                .categoryId("10001")
                .localeId("1042")
                .localizedName("quelque chose cool")
                .build();
        DomainCategory domainCategory3 = DomainCategory.builder()
                .parentCategoryId("1")
                .categoryId("30000")
                .localeId("1033")
                .localizedName("WOW")
                .build();
        DomainCategory domainCategory4 = DomainCategory.builder()
                .categoryId("0")
                .localeId("1033")
                .localizedName("things")
                .build();
        DomainCategory domainCategory5 = DomainCategory.builder()
                .categoryId("1")
                .localeId("1033")
                .localizedName("Exclamations")
                .build();
        DomainCategory domainCategory6 = DomainCategory.builder()
                .categoryId("0")
                .localeId("1066")
                .localizedName("sginht")
                .build();
        DomainCategory domainCategory7 = DomainCategory.builder()
                .parentCategoryId("0")
                .categoryId("10002")
                .localeId("1033")
                .localizedName("something bad")
                .build();
        domainCategoryList.addAll(Arrays.asList(domainCategory0, domainCategory1, domainCategory2, domainCategory3, domainCategory4, domainCategory5, domainCategory6, domainCategory7));
        List<Category> categoryList = domainCategoryListToCategoryList(domainCategoryList);
        assertTrue(3 == categoryList.size());
        assertEquals("0", categoryList.get(0).getCategoryId());
        assertTrue(2 == categoryList.get(0).getSubcategories().size());
        assertEquals("10001", categoryList.get(0).getSubcategories().get(0).getSubcategoryId());
        assertTrue(2 == categoryList.get(0).getSubcategories().get(0).getSubcategoryName().size());
        assertTrue(2 == categoryList.get(0).getCategoryName().size());
        assertEquals("1033", categoryList.get(0).getCategoryName().get(0).getLocaleId());
        assertEquals("1066", categoryList.get(0).getCategoryName().get(1).getLocaleId());
        assertEquals("1033", categoryList.get(0).getSubcategories().get(0).getSubcategoryName().get(0).getLocaleId());
        assertEquals("1042", categoryList.get(0).getSubcategories().get(0).getSubcategoryName().get(1).getLocaleId());
        assertEquals("10002", categoryList.get(0).getSubcategories().get(1).getSubcategoryId());
        assertTrue(1 == categoryList.get(0).getSubcategories().get(1).getSubcategoryName().size());
        assertEquals("1", categoryList.get(1).getCategoryId());
        assertTrue(1 == categoryList.get(1).getSubcategories().size());
        assertTrue(1 == categoryList.get(1).getCategoryName().size());
    }

    @Test
    public void setMediaByDomainIdQueryStringTestActiveFilter() {
        String baseQuery = "SELECT * FROM Media WHERE `domain-id` = ?";
        String result = setMediaByDomainIdQueryString(baseQuery, true, false, false, new String[]{});
        assertEquals("SELECT * FROM Media WHERE `domain-id` = ? AND `active` = ?", result);
    }

    @Test
    public void setMediaByDomainIdQueryStringTestActiveFilterAndPagination() {
        String baseQuery = "SELECT * FROM Media WHERE `domain-id` = ?";
        String result = setMediaByDomainIdQueryString(baseQuery, true, false, true, new String[]{});
        assertEquals("SELECT * FROM Media WHERE `domain-id` = ? AND `active` = ? LIMIT ?, ?", result);
    }

    @Test
    public void setMediaByDomainIdQueryStringTestActiveFilterAndDerivativeFilterAndPagination() {
        String baseQuery = "SELECT * FROM Media WHERE `domain-id` = ?";
        String result = setMediaByDomainIdQueryString(baseQuery, true, true, true, new String[]{"a","b","c"});
        assertEquals("SELECT * FROM Media WHERE `domain-id` = ? AND `active` = ? AND `derivative-category` IN (?,?,?) LIMIT ?, ?", result);
    }

    @Test
    public void setMediaByDomainIdQueryStringTestNoFiltersUsed() {
        String baseQuery = "SELECT * FROM Media WHERE `domain-id` = ?";
        String result = setMediaByDomainIdQueryString(baseQuery, false, false, false, new String[]{});
        assertEquals(baseQuery, result);
    }

    @Test
    public void buildMediaFromResultSetTest() throws Exception {
        when(mockResultSet.getString(eq("domain-fields"))).thenReturn("{\"rooms\":[{\"roomHero\":\"false\",\"roomId\":\"11140466\"},{\"roomHero\":\"false\"," +
                "\"roomId\":\"11140470\"},{\"roomHero\":\"false\",\"roomId\":\"11140478\"},{\"roomHero\":\"false\",\"roomId\":\"11140486\"},{\"roomHero\":\"false\"," +
                "\"roomId\":\"11140528\"},{\"roomHero\":\"false\",\"roomId\":\"11651502\"}],\"lcmMediaId\":14633796,\"subcategoryId\":\"81003\"}");
        when(mockResultSet.getString(eq("derivatives"))).thenReturn("[{\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249" +
                "/9547249_7_t.jpg\", \"type\": \"t\", \"width\": 70, \"height\": 70, \"fileSize\": 2048}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_s.jpg\", \"type\": \"s\", \"width\": 200, \"height\": 138, " +
                "\"fileSize\": 8192}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_b.jpg\", \"type\": \"b\", " +
                "\"width\": 350, \"height\": 241, \"fileSize\": 22528}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_l.jpg\", \"type\": \"l\", \"width\": 255, \"height\": 144, " +
                "\"fileSize\": 11264}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_n.jpg\", \"type\": \"n\", " +
                "\"width\": 90, \"height\": 90, \"fileSize\": 3072}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_g" +
                ".jpg\", \"type\": \"g\", \"width\": 140, \"height\": 140, \"fileSize\": 7168}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_d.jpg\", \"type\": \"d\", \"width\": 180, \"height\": 180, " +
                "\"fileSize\": 10240}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_y.jpg\", \"type\": \"y\", " +
                "\"width\": 500, \"height\": 345, \"fileSize\": 43008}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_z.jpg\", \"type\": \"z\", \"width\": 1000, \"height\": 690, " +
                "\"fileSize\": 142336}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_e.jpg\", \"type\": \"e\", " +
                "\"width\": 160, \"height\": 90, \"fileSize\": 5120}]");
        when(mockResultSet.getString(eq("fingerprints"))).thenReturn("[{\"algorithm\": \"pHash\", \"values\": [\"0011000110011001110111100011111001111010010111101\"]}, " +
                "{\"algorithm\": \"SHA1\", \"values\": [\"6AB477AF6944298431795A11F48CCA4F55B154E4\"]}]");
        when(mockResultSet.getString(eq("guid"))).thenReturn("0000041d-4119-4b61-867c-9c758c8b94d3");
        when(mockResultSet.getString(eq("file-url"))).thenReturn("s3://ewe-cs-media-test/test/source/lodging/10000000/9550000/9547300/9547249/9547249_7.jpg");
        when(mockResultSet.getString(eq("file-name"))).thenReturn("9547249_00005fx1.jpg");
        when(mockResultSet.getInt(eq("file-size"))).thenReturn(192512);
        when(mockResultSet.getInt(eq("width"))).thenReturn(1210);
        when(mockResultSet.getInt(eq("height"))).thenReturn(835);
        when(mockResultSet.getString(eq("source-url"))).thenReturn("s3://ewe-cs-media-test/test/source/lodging/10000000/9550000/9547300/9547249/9547249_7.jpg");
        when(mockResultSet.getString(eq("domain"))).thenReturn("Lodging");
        when(mockResultSet.getString(eq("domain-id"))).thenReturn("9547249");
        when(mockResultSet.getTimestamp(eq("update-date"))).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(mockResultSet.getInt(eq("active"))).thenReturn(1);
        when(mockResultSet.getString(eq("provider"))).thenReturn("Expedia");
        when(mockResultSet.getString(eq("client-id"))).thenReturn("LCMMigrationScript");
        when(mockResultSet.getString(eq("user-id"))).thenReturn("MediaImport");
        when(mockResultSet.getString(eq("metadata"))).thenReturn(null);
        when(mockResultSet.getString(eq("comments"))).thenReturn("Spa");
        when(mockResultSet.getString(eq("status"))).thenReturn("PUBLISHED");
        when(mockResultSet.getString(eq("derivative-category"))).thenReturn(null);
        when(mockResultSet.getInt(eq("hidden"))).thenReturn(0);
        when(mockResultSet.getString(eq("provided-name"))).thenReturn(null);
        Optional<Media> result = buildMediaFromResultSet(mockResultSet);
        assertTrue(result.isPresent());
        Media resultMedia = result.get();
        assertEquals("0000041d-4119-4b61-867c-9c758c8b94d3", resultMedia.getMediaGuid());
        assertEquals("true", resultMedia.getActive());
        assertEquals("Lodging", resultMedia.getDomain());
        assertEquals("9547249", resultMedia.getDomainId());
        assertTrue(192512 == resultMedia.getFileSize());
        assertNotNull(resultMedia.getDomainFields());
        assertEquals("14633796", resultMedia.getLcmMediaId());
        assertNotNull(resultMedia.getDerivatives());
    }

    @Test
    public void buildMediaFromResultSetTestError() throws Exception {
        when(mockResultSet.getString(anyString())).thenThrow(new SQLException());
        when(mockResultSet.toString()).thenReturn("{}");
        Optional<Media> result = buildMediaFromResultSet(mockResultSet);
        assertFalse(result.isPresent());
    }

    @Test
    public void buildDomainIdMediaFromResultSetTestWithFilter() throws Exception {
        when(mockResultSet.getString(eq("domain-fields"))).thenReturn("{\"rooms\":[{\"roomHero\":\"false\",\"roomId\":\"11140466\"},{\"roomHero\":\"false\"," +
                "\"roomId\":\"11140470\"},{\"roomHero\":\"false\",\"roomId\":\"11140478\"},{\"roomHero\":\"false\",\"roomId\":\"11140486\"},{\"roomHero\":\"false\"," +
                "\"roomId\":\"11140528\"},{\"roomHero\":\"false\",\"roomId\":\"11651502\"}],\"lcmMediaId\":14633796,\"subcategoryId\":\"81003\"}");
        when(mockResultSet.getString(eq("derivatives"))).thenReturn("[{\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249" +
                "/9547249_7_t.jpg\", \"type\": \"t\", \"width\": 70, \"height\": 70, \"fileSize\": 2048}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_s.jpg\", \"type\": \"s\", \"width\": 200, \"height\": 138, " +
                "\"fileSize\": 8192}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_b.jpg\", \"type\": \"b\", " +
                "\"width\": 350, \"height\": 241, \"fileSize\": 22528}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_l.jpg\", \"type\": \"l\", \"width\": 255, \"height\": 144, " +
                "\"fileSize\": 11264}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_n.jpg\", \"type\": \"n\", " +
                "\"width\": 90, \"height\": 90, \"fileSize\": 3072}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_g" +
                ".jpg\", \"type\": \"g\", \"width\": 140, \"height\": 140, \"fileSize\": 7168}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_d.jpg\", \"type\": \"d\", \"width\": 180, \"height\": 180, " +
                "\"fileSize\": 10240}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_y.jpg\", \"type\": \"y\", " +
                "\"width\": 500, \"height\": 345, \"fileSize\": 43008}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_z.jpg\", \"type\": \"z\", \"width\": 1000, \"height\": 690, " +
                "\"fileSize\": 142336}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_e.jpg\", \"type\": \"e\", " +
                "\"width\": 160, \"height\": 90, \"fileSize\": 5120}]");
        when(mockResultSet.getString(eq("fingerprints"))).thenReturn("[{\"algorithm\": \"pHash\", \"values\": [\"0011000110011001110111100011111001111010010111101\"]}, " +
                "{\"algorithm\": \"SHA1\", \"values\": [\"6AB477AF6944298431795A11F48CCA4F55B154E4\"]}]");
        when(mockResultSet.getString(eq("guid"))).thenReturn("0000041d-4119-4b61-867c-9c758c8b94d3");
        when(mockResultSet.getString(eq("file-url"))).thenReturn("s3://ewe-cs-media-test/test/source/lodging/10000000/9550000/9547300/9547249/9547249_7.jpg");
        when(mockResultSet.getString(eq("file-name"))).thenReturn("9547249_00005fx1.jpg");
        when(mockResultSet.getInt(eq("file-size"))).thenReturn(192512);
        when(mockResultSet.getInt(eq("width"))).thenReturn(1210);
        when(mockResultSet.getInt(eq("height"))).thenReturn(835);
        when(mockResultSet.getString(eq("source-url"))).thenReturn("s3://ewe-cs-media-test/test/source/lodging/10000000/9550000/9547300/9547249/9547249_7.jpg");
        when(mockResultSet.getString(eq("domain"))).thenReturn("Lodging");
        when(mockResultSet.getString(eq("domain-id"))).thenReturn("9547249");
        when(mockResultSet.getTimestamp(eq("update-date"))).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(mockResultSet.getInt(eq("active"))).thenReturn(1);
        when(mockResultSet.getString(eq("provider"))).thenReturn("Expedia");
        when(mockResultSet.getString(eq("client-id"))).thenReturn("LCMMigrationScript");
        when(mockResultSet.getString(eq("user-id"))).thenReturn("MediaImport");
        when(mockResultSet.getString(eq("metadata"))).thenReturn(null);
        when(mockResultSet.getString(eq("comments"))).thenReturn("Spa");
        when(mockResultSet.getString(eq("status"))).thenReturn("PUBLISHED");
        when(mockResultSet.getString(eq("derivative-category"))).thenReturn(null);
        when(mockResultSet.getInt(eq("hidden"))).thenReturn(0);
        when(mockResultSet.getString(eq("provided-name"))).thenReturn(null);
        Optional<DomainIdMedia> result = buildDomainIdMediaFromResultSet(mockResultSet, true, "b,n");
        assertTrue(result.isPresent());
        DomainIdMedia resultMedia = result.get();
        assertEquals("0000041d-4119-4b61-867c-9c758c8b94d3", resultMedia.getMediaGuid());
        assertEquals("true", resultMedia.getActive());
        assertTrue(192512 == resultMedia.getFileSize());
        assertNotNull(resultMedia.getDomainFields());
        assertNotNull(resultMedia.getDerivatives());
        assertTrue(2 == resultMedia.getDerivatives().size());
        assertEquals("b", resultMedia.getDerivatives().get(0).get("type"));
        assertEquals("n", resultMedia.getDerivatives().get(1).get("type"));
    }

    @Test
    public void buildDomainIdMediaFromResultSetTestWithoutFilter() throws Exception {
        when(mockResultSet.getString(eq("domain-fields"))).thenReturn("{\"rooms\":[{\"roomHero\":\"false\",\"roomId\":\"11140466\"},{\"roomHero\":\"false\"," +
                "\"roomId\":\"11140470\"},{\"roomHero\":\"false\",\"roomId\":\"11140478\"},{\"roomHero\":\"false\",\"roomId\":\"11140486\"},{\"roomHero\":\"false\"," +
                "\"roomId\":\"11140528\"},{\"roomHero\":\"false\",\"roomId\":\"11651502\"}],\"lcmMediaId\":14633796,\"subcategoryId\":\"81003\"}");
        when(mockResultSet.getString(eq("derivatives"))).thenReturn("[{\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249" +
                "/9547249_7_t.jpg\", \"type\": \"t\", \"width\": 70, \"height\": 70, \"fileSize\": 2048}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_s.jpg\", \"type\": \"s\", \"width\": 200, \"height\": 138, " +
                "\"fileSize\": 8192}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_b.jpg\", \"type\": \"b\", " +
                "\"width\": 350, \"height\": 241, \"fileSize\": 22528}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_l.jpg\", \"type\": \"l\", \"width\": 255, \"height\": 144, " +
                "\"fileSize\": 11264}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_n.jpg\", \"type\": \"n\", " +
                "\"width\": 90, \"height\": 90, \"fileSize\": 3072}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_g" +
                ".jpg\", \"type\": \"g\", \"width\": 140, \"height\": 140, \"fileSize\": 7168}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_d.jpg\", \"type\": \"d\", \"width\": 180, \"height\": 180, " +
                "\"fileSize\": 10240}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_y.jpg\", \"type\": \"y\", " +
                "\"width\": 500, \"height\": 345, \"fileSize\": 43008}, {\"location\": " +
                "\"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_z.jpg\", \"type\": \"z\", \"width\": 1000, \"height\": 690, " +
                "\"fileSize\": 142336}, {\"location\": \"s3://ewe-cs-media-test/test/derivative/lodging/10000000/9550000/9547300/9547249/9547249_7_e.jpg\", \"type\": \"e\", " +
                "\"width\": 160, \"height\": 90, \"fileSize\": 5120}]");
        when(mockResultSet.getString(eq("fingerprints"))).thenReturn("[{\"algorithm\": \"pHash\", \"values\": [\"0011000110011001110111100011111001111010010111101\"]}, " +
                "{\"algorithm\": \"SHA1\", \"values\": [\"6AB477AF6944298431795A11F48CCA4F55B154E4\"]}]");
        when(mockResultSet.getString(eq("guid"))).thenReturn("0000041d-4119-4b61-867c-9c758c8b94d3");
        when(mockResultSet.getString(eq("file-url"))).thenReturn("s3://ewe-cs-media-test/test/source/lodging/10000000/9550000/9547300/9547249/9547249_7.jpg");
        when(mockResultSet.getString(eq("file-name"))).thenReturn("9547249_00005fx1.jpg");
        when(mockResultSet.getInt(eq("file-size"))).thenReturn(192512);
        when(mockResultSet.getInt(eq("width"))).thenReturn(1210);
        when(mockResultSet.getInt(eq("height"))).thenReturn(835);
        when(mockResultSet.getString(eq("source-url"))).thenReturn("s3://ewe-cs-media-test/test/source/lodging/10000000/9550000/9547300/9547249/9547249_7.jpg");
        when(mockResultSet.getString(eq("domain"))).thenReturn("Lodging");
        when(mockResultSet.getString(eq("domain-id"))).thenReturn("9547249");
        when(mockResultSet.getTimestamp(eq("update-date"))).thenReturn(new Timestamp(System.currentTimeMillis()));
        when(mockResultSet.getInt(eq("active"))).thenReturn(1);
        when(mockResultSet.getString(eq("provider"))).thenReturn("Expedia");
        when(mockResultSet.getString(eq("client-id"))).thenReturn("LCMMigrationScript");
        when(mockResultSet.getString(eq("user-id"))).thenReturn("MediaImport");
        when(mockResultSet.getString(eq("metadata"))).thenReturn(null);
        when(mockResultSet.getString(eq("comments"))).thenReturn("Spa");
        when(mockResultSet.getString(eq("status"))).thenReturn("PUBLISHED");
        when(mockResultSet.getString(eq("derivative-category"))).thenReturn(null);
        when(mockResultSet.getInt(eq("hidden"))).thenReturn(0);
        when(mockResultSet.getString(eq("provided-name"))).thenReturn(null);
        Optional<DomainIdMedia> result = buildDomainIdMediaFromResultSet(mockResultSet, false, null);
        assertTrue(result.isPresent());
        DomainIdMedia resultMedia = result.get();
        assertEquals("0000041d-4119-4b61-867c-9c758c8b94d3", resultMedia.getMediaGuid());
        assertEquals("true", resultMedia.getActive());
        assertTrue(192512 == resultMedia.getFileSize());
        assertNotNull(resultMedia.getDomainFields());
        assertNotNull(resultMedia.getDerivatives());
        assertTrue(10 == resultMedia.getDerivatives().size());
    }

    @Test
    public void buildDomainIdMediaFromResultSetTestError() throws Exception {
        when(mockResultSet.getString(anyString())).thenThrow(new SQLException());
        when(mockResultSet.toString()).thenReturn("{}");
        Optional<DomainIdMedia> result = buildDomainIdMediaFromResultSet(mockResultSet, false, null);
        assertFalse(result.isPresent());
    }

}
