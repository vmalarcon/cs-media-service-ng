package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.DomainIdMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An interface for all DAOs implementations to adhere to for accessing media.
 */
public interface MediaDao {

    /**
     * Retrieve media items for a domain item.
     *
     * @param domain                    Domain the item belongs too.
     * @param domainId                  The id of the domain item media items are needed.
     * @param activeFilter              Filters active or inactive media. If "all" or null is provided all items are returned.
     * @param derivativeFilter          Inclusive filter of derivatives. A null or empty string will not exclude any derivatives.
     * @param derivativeCategoryFilter  Inclusive filter of media. A null or empty string will not exclude any media.
     * @param pageSize                  Positive integer to filter the number of media displayed per page. pageSize is inclusive with pageIndex.
     * @param pageIndex                 Positive integer to filter the page to display. pageIndex is inclusive with pageSize.
     * @return List of media that belongs to the domain item.
     */
    List<Optional<DomainIdMedia>> getMediaByDomainId(Domain domain, String domainId, String activeFilter, String derivativeFilter, String derivativeCategoryFilter,
                                                     Integer pageSize, Integer pageIndex);


    Optional<Integer> getTotalMediaCountByDomainId(Domain domain, String domainId, String activeFilter, String derivativeCategoryFilter);

    /**
     * Given a fileName returns all the media that were saved with that same name.
     *
     * @param fileName File name of the Media.
     * @return List of Media with the requested Filename.
     */
    List<Optional<Media>> getMediaByFilename(String fileName);

    /**
     * Deletes a media by its GUID.
     *
     * @param mediaGUID GUID of the media to delete.
     */
    void deleteMediaByGUID(String mediaGUID);

    /**
     * get a Media record from DB by GUID
     *
     * @param mediaGUID
     * @return
     */
    Optional<Media> getMediaByGuid(String mediaGUID);

    /**
     * get Media list from MediaDB by lcmMedia ID.
     * @param mediaId
     * @return
     */
    List<Optional<Media>> getMediaByMediaId(String mediaId);

    /**
     * get MediaList by domainId
     * @param domainId
     * @return
     */
    List<Optional<Media>> getMediaByDomainId(String domainId);

    /**
     * set propertyHero to false in media 'domainFields'
     * @param mediaGUID
     * @param domainField
     */
    void unheroMedia(String mediaGUID, String domainField);

    /**
     * store imageMessage in media DB
     *
     * @param message
     * @throws Exception
     */
    void addMedia(ImageMessage message) throws Exception;

    /**
     * update imageMessage in mediaDB
     *
     * @param message
     * @throws Exception
     */
     void updateMedia(ImageMessage message) throws Exception;

    /**
     * Pulls the latest processing status of media files. When a file doesn't have any process logs the file is
     * considered old and therefore published.
     *
     * @param fileNames File names for which the status is required.
     * @return The latest status of a media file.
     */
    Map<String, String> getLatestStatus(List<String> fileNames);

    /**
     * Pulls the latest processing status of media files.
     *
     * @param fileNames media file name list.
     * @return the media status object
     */
    List<MediaProcessLog> findMediaStatus(final List<String> fileNames);
}
