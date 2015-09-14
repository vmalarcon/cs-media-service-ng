package com.expedia.content.media.processing.services.dao;


import java.util.List;

/**
 * Methods for reporting and supporting reporting.
 */
public interface LcmProcessLogDao {

    /**
     *
     * @param fileNameList media file name list.
     * @return the media status object
     */
    List<MediaProcessLog> findMediaStatus(final List<String> fileNameList);

}
