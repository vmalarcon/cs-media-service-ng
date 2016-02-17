package com.expedia.content.media.processing.services.dao;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SKUGroupCatalogItem DAO.
 */
@Component
public class SKUGroupCatalogItemDao {
    private final SKUGroupGetSproc sproc;

    @Autowired
    public SKUGroupCatalogItemDao(SKUGroupGetSproc sproc) {
        this.sproc = sproc;
    }

    /**
     * queries LCM and looks for the given skuGroupCatalogItemID
     * @param skuGroupCatalogItemID
     * @return true if the skuGroupCatalogItemID exists
     */
    @SuppressWarnings("unchecked")
    public Boolean skuGroupExists(int skuGroupCatalogItemID) {
        final Map<String, Object> results = sproc.execute(skuGroupCatalogItemID);
        final List<SKUGroupCatalogItem> skuGroupCatalogItems = (List<SKUGroupCatalogItem>) results.get(SKUGroupGetSproc.SKU_GROUP_CATALOG_ITEM_MAPPER_RESULT_SET);

        return CollectionUtils.isNotEmpty(skuGroupCatalogItems);
    }
}
