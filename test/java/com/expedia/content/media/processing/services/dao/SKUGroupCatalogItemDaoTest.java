package com.expedia.content.media.processing.services.dao;


import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SKUGroupCatalogItemDaoTest {

    @BeforeClass
    public static void setUp() {
        System.setProperty("EXPEDIA_ENVIRONMENT", "test");
        System.setProperty("AWS_REGION", "us-west-2");
    }

    @Test
    public void testDomainIdExists() throws Exception {
        List<SKUGroupCatalogItem> skuGroupCatalogItems = new ArrayList<>();
        SKUGroupCatalogItem skuGroupCatalogItem = new SKUGroupCatalogItem("Rodeway Inn Perry", 0, 32.473380D,
                -83.746030D, Boolean.FALSE, 1, "ATL", Boolean.FALSE, "", 1, new Timestamp(1339150200000L),
                "PSGMcunningham", new Timestamp(1339150200000L), "PSGJambrose", 2);
        skuGroupCatalogItems.add(skuGroupCatalogItem);
        SKUGroupGetSproc mockSkuGroupGet = mock(SKUGroupGetSproc.class);
        SKUGroupCatalogItemDao skuGroupCatalogItemDao = new SKUGroupCatalogItemDao(mockSkuGroupGet);
        Map<String, Object> mockResults = new HashMap<>();
        mockResults.put(SKUGroupGetSproc.SKU_GROUP_CATALOG_ITEM_MAPPER_RESULT_SET, skuGroupCatalogItems);
        when(mockSkuGroupGet.execute(123)).thenReturn(mockResults);
        assertTrue(skuGroupCatalogItemDao.skuGroupExists(123));
    }


    @Test
    public void testDomainIdDoesNotExist() throws Exception {
        List<SKUGroupCatalogItem> skuGroupCatalogItems = new ArrayList<>();
        SKUGroupGetSproc mockSkuGroupGet = mock(SKUGroupGetSproc.class);
        SKUGroupCatalogItemDao skuGroupCatalogItemDao = new SKUGroupCatalogItemDao(mockSkuGroupGet);
        Map<String, Object> mockResults = new HashMap<>();
        mockResults.put(SKUGroupGetSproc.SKU_GROUP_CATALOG_ITEM_MAPPER_RESULT_SET, skuGroupCatalogItems);
        when(mockSkuGroupGet.execute(123)).thenReturn(mockResults);
        assertFalse(skuGroupCatalogItemDao.skuGroupExists(123));
    }
}
