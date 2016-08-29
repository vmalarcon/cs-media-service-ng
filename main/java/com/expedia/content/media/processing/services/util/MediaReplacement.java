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
    private static final String  GUID_PATTERN ="(.*)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(.*)";

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
                //if it is GUID filename, do not need to check provider.
                .filter(m -> provider.equalsIgnoreCase(m.getProvider()) || m.getFileName().matches(GUID_PATTERN))
                .max((m1, m2) -> m1.getLastUpdated().compareTo(m2.getLastUpdated()));
    }

    public static Optional<Media> selectLatestMedia(List<Media> mediaList) {
        return mediaList.stream()
                .max((m1, m2) -> m1.getLastUpdated().compareTo(m2.getLastUpdated()));
    }
}
