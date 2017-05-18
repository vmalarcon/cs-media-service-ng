package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.services.dao.domain.MediaDerivative;

public interface DerivativesDao {

    MediaDerivative getDerivativeByMediaGuid(String mediaGuid);

    /**
     *
     * @param location
     * @return
     */
    MediaDerivative getDerivativeByLocation(String location);
}
