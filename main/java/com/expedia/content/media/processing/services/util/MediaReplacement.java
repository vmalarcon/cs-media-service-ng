package com.expedia.content.media.processing.services.util;

import java.util.List;
import java.util.Optional;

import com.expedia.content.media.processing.services.dao.domain.Media;

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
    public static Optional<Media> selectBestMedia(List<Media> mediaList) {
        return mediaList.stream()
                .filter(m -> m.getActive().equalsIgnoreCase("true"))
                .max((m1, m2) -> m1.getLastUpdated().compareTo(m2.getLastUpdated()));
    }
}
