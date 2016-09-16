package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.exception.PaginationValidationException;
import com.expedia.content.media.processing.services.reqres.MediaByDomainIdResponse;
import com.expedia.content.media.processing.services.reqres.MediaGetResponse;

import java.util.List;
import java.util.Map;


public interface MediaDao {

    /**
     * Retrieve media items for a domain item.
     *
     * @param domain           Domain the item belongs too.
     * @param domainId         The id of the domain item media items are needed.
     * @param activeFilter     Filters active or inactive media. If "all" or null is provided all items are returned.
     * @param derivativeFilter Inclusive filter of derivatives. A null or empty string will not exclude any derivatives.
     * @param derivativeCategoryFilter Inclusive filter of media. A null or empty string will not exclude any media.
     * @param pageSize         Positive integer to filter the number of media displayed per page. pageSize is inclusive with pageIndex.
     * @param pageIndex        Positive integer to filter the page to display. pageIndex is inclusive with pageSize.
     * @return List of media that belongs to the domain item.
     */
    MediaByDomainIdResponse getMediaByDomainId(Domain domain, String domainId, String activeFilter, String derivativeFilter, String derivativeCategoryFilter, Integer pageSize, Integer pageIndex) throws Exception,
            PaginationValidationException;

    /**
     * Given a fileName returns all the media that were saved with that name.
     *
     * @param fileName File name of the Media.
     * @return List of Media with the requested Filename.
     */
    List<Media> getMediaByFilename(String fileName);

    /**
     * Given a fileName returns all the media that were saved with that name by query LCM.
     *
     * @param fileName File name of the Media.
     * @return List of Media with the requested Filename.
     */
    List<LcmMedia> getMediaByFilenameInLCM(int domainId, String fileName);

    /**
     * Retrieves a media by its GUID.
     *
     * @param mediaGUID GUID of the required media.
     * @return Media with the requested GUID.
     */
    MediaGetResponse getMediaByGUID(String mediaGUID);

    /**
     * Deletes a media by its GUID.
     *
     * @param mediaGUID GUID of the media to delete.
     */
    void deleteMediaByGUID(String mediaGUID);

    /**
     * get the domainId and ContentProviderMediaName from DB by derivative file name.
     *
     * @param fileName
     * @return
     */
    LcmMedia getContentProviderName(String fileName);

    /**
     * get Media info from Dynamo by GUID
     * @param guid
     * @return
     */
    Media getMediaByGuid(String guid);

    /**
     * get Media list from Dynamo by lcmMedia ID.
     * @param mediaId
     * @return
     */
    List<Media> getMediaByMediaId(String mediaId);

    /**
     * save media info to dynamo Media table.
     * @param media
     */
    void saveMedia(Media media);

    /**
     * Pulls the latest processing status of media files. When a file doesn't have any process logs the file is
     * considered old and therefore published.
     *
     * @param fileNames File names for which the status is required.
     * @return The latest status of a media file.
     */
    Map<String, String> getLatestStatus(List<String> fileNames);
}
