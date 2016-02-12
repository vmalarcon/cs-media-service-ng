package com.expedia.content.media.processing.services.dao;


import java.util.List;

import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;

/**
 * Methods for reporting and supporting reporting.
 */
public interface ProcessLogDao {

    /**
     * get the media image status from DB
     *
     * @param fileNameList media file name list.
     * @return the media status object
     */
    List<MediaProcessLog> findMediaStatus(final List<String> fileNameList);

}
