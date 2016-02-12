package com.expedia.content.media.processing.services.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MediaProvider DAO.
 */
@Component
public class MediaProviderDao {
    private final MediaProviderSproc sproc;

    @Autowired
    public MediaProviderDao(MediaProviderSproc sproc) {
        this.sproc = sproc;
    }

    /**
     * @param mediaProviderID
     * @param mediaProviderName
     * @return true if the mediaProvider exists in LCM
     */
    @SuppressWarnings("unchecked")
    public Boolean getMediaProviderList(Integer mediaProviderID, String mediaProviderName) {
        final Map<String, Object> results = sproc.execute();
        final List<MediaProvider> mediaProviders = (List<MediaProvider>) results.get(MediaProviderSproc.MEDIA_PROVIDER_MAPPER_RESULT_SET);
        List<Integer> mediaProviderIds = mediaProviders.stream()
                .map(mediaProvider -> mediaProvider.getMediaProviderID())
                .collect(Collectors.toList());
        List<String> mediaProviderNames = mediaProviders.stream()
                .map(mediaProvider -> mediaProvider.getMediaProviderName())
                .collect(Collectors.toList());
        return ((mediaProviderID != null && mediaProviderIds.contains(mediaProviderID)) ||
                (mediaProviderName != null && mediaProviderNames.contains(mediaProviderName)));
    }
}
