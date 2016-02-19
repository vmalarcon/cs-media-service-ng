package com.expedia.content.media.processing.services.validator;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.google.common.base.Splitter;

/**
 * Utility methods/logic for the Media replacement logic.
 *
 * <p>The class reads a configuration property with the list (comma separated) of Providers that need the replacement logic</p>
 * <p>This class also provides utility methods to deal with the selection of media when multiple are found for replacement</p>
 */
@Component
public class MediaReplacement {

    private final String providersWithReplace;

    @Autowired
    public MediaReplacement(@Value("${providers.with.replace}") String providersWithReplace) {
        this.providersWithReplace = providersWithReplace;
    }

    /**
     * Inspects the message to figure out if this is a replacement or not.
     *
     * <p>Replacement messages will typically have a replace flag in the domain data of the ImageMessage</p>
     *
     * @param imageMessage ImageMessage to look for the replace flag
     * @return If this message is a replacement returns true, otherwise false.
     */
    public boolean isReplacement(ImageMessage imageMessage) {
        if (imageMessage.getOuterDomainData() != null) {
            final String provider = imageMessage.getOuterDomainData().getProvider();
            return Splitter.on(",").omitEmptyStrings().trimResults().splitToList(providersWithReplace).stream()
                    .anyMatch(p -> p.equals(provider));
        }
        return false;
    }

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
