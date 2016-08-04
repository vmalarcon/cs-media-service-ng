package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.dao.domain.Media;

import java.util.List;
import java.util.Optional;

/**
 * Utility methods/logic for the Media replacement logic.
 *
 * <p>The class reads a configuration property with the list (comma separated) of Providers that need the replacement logic</p>
 * <p>This class also provides utility methods to deal with the selection of media when multiple are found for replacement</p>
 */
public class MediaReplacement {

    private MediaReplacement() {}
    
    /**
     * Goes through all the active media in search of the latest media record. This is considered to be the 'best'
     * media.
     *
     * @param mediaList List of media from which to select the best.
     * @return Best media according to the logic
     */
    public static Optional<Media> selectBestMedia(List<Media> mediaList, String domainId, String provider) {
        return mediaList.stream()
                .filter(m -> m.getDomainId().equalsIgnoreCase(domainId))
                //avoid NPE when legacy data record in dynamo does not have "provider".
                .filter(m -> provider.equalsIgnoreCase(m.getProvider()))
                .max((m1, m2) -> m1.getLastUpdated().compareTo(m2.getLastUpdated()));
    }

    public static Optional<Media> selectLatestMedia(List<Media> mediaList) {
        return mediaList.stream()
                .max((m1, m2) -> m1.getLastUpdated().compareTo(m2.getLastUpdated()));
    }
}
