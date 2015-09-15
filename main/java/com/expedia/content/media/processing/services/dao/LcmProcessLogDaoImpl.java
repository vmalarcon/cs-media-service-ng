package com.expedia.content.media.processing.services.dao;

import com.expedia.content.metrics.aspects.annotations.Meter;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * LCM implementations of process log DAO interface.
 */
@Component
public class LcmProcessLogDaoImpl implements LcmProcessLogDao {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS XXX";

    private final String processName;

    private final SQLMediaLogSproc sqlMediaLogSproc;

    @Autowired
    public LcmProcessLogDaoImpl(
            @Value("${processname}") final String processName, final SQLMediaLogSproc sqlMediaLogSproc) {
        this.processName = processName;
        this.sqlMediaLogSproc = sqlMediaLogSproc;
    }


    @Override
    @SuppressWarnings("unchecked")
    @Meter(name = "findMediaStatus")
    public List<MediaProcessLog> findMediaStatus(List<String> fileNameList) {
        if (fileNameList != null) {
            String fileNameAll = StringUtils.join(fileNameList, ";");
            Map<String, Object> results = sqlMediaLogSproc.execute(fileNameAll);
            List<MediaProcessLog> mediaLogStatuses = (List<MediaProcessLog>) results.get(SQLMediaLogSproc.MEDIAS_RESULT_SET);
            if (!mediaLogStatuses.isEmpty()) {
                return mediaLogStatuses;
            }
        }
        return null;
    }

}
