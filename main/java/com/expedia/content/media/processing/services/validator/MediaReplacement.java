package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.Media;

import java.util.List;
import java.util.Optional;

/**
 * Utility methods/logic for the Media replacement logic
 */
public final class MediaReplacement {

    public static final String REPLACE_FIELD = "replace";

    private MediaReplacement() {
        // static class
    }

    /**
     * Inspects the message to figure out if this is a replacement or not.
     *
     * <p>Replacement messages will typically have a replace flag in the domain data of the ImageMessage</p>
     *
     * @param imageMessage ImageMessage to look for the replace flag
     * @return If this message is a replacement returns true, otherwise false.
     */
    public static boolean isReplacement(ImageMessage imageMessage) {
        if (imageMessage.getOuterDomainData() == null || imageMessage.getOuterDomainData().getDomainFields() == null) {
            return false;
        }
        final Object replace = imageMessage.getOuterDomainData().getDomainFieldValue(REPLACE_FIELD);
        if (replace instanceof String) {
            return Boolean.parseBoolean((String) replace);
        }
        return false;
    }

    /**
     * Goes thru all the active media in search of the latest media record. This is considered to be the 'best'
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
