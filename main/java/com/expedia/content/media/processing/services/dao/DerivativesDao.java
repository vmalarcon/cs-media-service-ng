package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.services.dao.domain.MediaDerivative;

import java.util.Optional;

//TODO: JavaDoc ALL the things
public interface DerivativesDao {

    Optional<MediaDerivative> getDerivativeByMediaGuid(String mediaGuid);

    /**
     *
     * @param location
     * @return
     */
    Optional<MediaDerivative> getDerivativeByLocation(String location);
}
