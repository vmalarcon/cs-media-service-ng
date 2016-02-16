package com.expedia.content.media.processing.services.dao;

import java.util.List;

/**
 * Data Access Object to perform queries for Media
 */
public interface MediaDAO {

    /**
     * Given a fileName returns all the media that were saved with that name.
     *
     * @param fileName File name of the Media.
     * @return List of Media with the requested Filename.
     */
    List<Media> getMediaByFilename(String fileName);
}
