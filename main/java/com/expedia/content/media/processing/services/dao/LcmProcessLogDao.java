package com.expedia.content.media.processing.services.dao;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaLogSproc;

import expedia.content.solutions.metrics.annotations.Meter;

/**
 * LCM implementations of process log DAO interface.
 */
@Component
public class LcmProcessLogDao implements ProcessLogDao {

    private final SQLMediaLogSproc sqlMediaLogSproc;

    @Autowired
    public LcmProcessLogDao(final SQLMediaLogSproc sqlMediaLogSproc) {
        this.sqlMediaLogSproc = sqlMediaLogSproc;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Meter(name = "findMediaStatus")
    public List<MediaProcessLog> findMediaStatus(final List<String> fileNameList) {
        if (fileNameList != null) {
            final String fileNameAll = StringUtils.join(fileNameList, ";");
            final Map<String, Object> results = sqlMediaLogSproc.execute(fileNameAll);
            final List<MediaProcessLog> mediaLogStatuses = (List<MediaProcessLog>) results.get(SQLMediaLogSproc.MEDIAS_RESULT_SET);
            if (!mediaLogStatuses.isEmpty()) {
                return mediaLogStatuses;
            }
        }
        return null;
    }

}
