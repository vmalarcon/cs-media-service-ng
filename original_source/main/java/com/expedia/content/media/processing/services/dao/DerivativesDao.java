package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.services.dao.domain.MediaDerivative;

import java.util.Optional;

/**
 * An interface for all DAOs implementations to adhere to for accessing derivative records.
 */
public interface DerivativesDao {

    /**
     *
     *
     * @param mediaGuid
     * @return
     */
    Optional<MediaDerivative> getDerivativeByMediaGuid(String mediaGuid);

    /**
     *
     *
     * @param location
     * @return
     */
    Optional<MediaDerivative> getDerivativeByLocation(String location);
}
