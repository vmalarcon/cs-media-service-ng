package com.expedia.content.media.processing.services.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Domain Categories DAO.
 */
@Component
public class CatalogItemMediaDao {
    private final CatalogItemMediaLstSproc sproc;

    @Autowired
    public CatalogItemMediaDao(CatalogItemMediaLstSproc sproc) {
        this.sproc = sproc;
    }

    /**
     * queries LCM and looks for the given catalogItemMediaId
     * @param catalogItemMediaId
     * @return true if the catalogItemMediaId exists
     */
    @SuppressWarnings("unchecked")
    public Boolean getCatalogItemMediaList(int catalogItemMediaId) {
        final Map<String, Object> results = sproc.execute(catalogItemMediaId);
        final List<CatalogItemMedia> catalogItemMediaList = (List<CatalogItemMedia>) results.get(CatalogItemMediaLstSproc.CatalogItemMedia_RESULT_SET);

        return !CollectionUtils.isEmpty(catalogItemMediaList);
    }
}
