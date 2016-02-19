package com.expedia.content.media.processing.services.dao;

import java.util.List;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.services.dao.domain.Media;

/**
 * Media data access operations.
 */
public interface MediaDao {

    /**
     * Retrieve media items for a domain item. 
     * 
     * @param domain Domain the item belongs too.
     * @param domainId The id of the domain item media items are needed.
     * @param activeFilter Filters active or inactive media. If "all" or null is provided all items are returned.
     * @param derivativeFilter Inclusive filter of derivatives. A null or empty string will not exclude any derivatives.
     * @return List of media that belongs to the domain item.
     */
    List<Media> getMediaByDomainId(Domain domain, String domainId, String activeFilter, String derivativeFilter);

}
