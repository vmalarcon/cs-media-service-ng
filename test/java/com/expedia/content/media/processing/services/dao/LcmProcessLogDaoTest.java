package com.expedia.content.media.processing.services.dao;

import org.junit.Test;

import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaLogSproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LcmProcessLogDaoTest {

    @Test
    public void testMediaStatusFound() {
        SQLMediaLogSproc mockMediaProcessLogSproc = mock(SQLMediaLogSproc.class);
        LcmProcessLogDao lcmProcessLogDao = new LcmProcessLogDao(mockMediaProcessLogSproc);
        String searchName = "1037678_109010ice.jpg";
        List<String> fileList = new ArrayList<>();
        fileList.add(searchName);
        Map<String, Object> value = new HashMap<String, Object>();
        List<MediaProcessLog>
                mediaLogStatuses = new ArrayList<MediaProcessLog>();
        MediaProcessLog
                mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "Publish", "1037678_109010ice.jpg", "Lodging");
        mediaLogStatuses.add(mediaLogStatus);
        value.put(SQLMediaLogSproc.MEDIAS_RESULT_SET, mediaLogStatuses);
        when(mockMediaProcessLogSproc.execute(searchName)).thenReturn(value);
        List<MediaProcessLog> mediaLogStatuses1 = lcmProcessLogDao.findMediaStatus(fileList);
        assertEquals(mediaLogStatuses1.get(0), mediaLogStatuses.get(0));
    }

    @Test
    public void testMediaStatusNotFound() {
        SQLMediaLogSproc mockMediaProcessLogSproc = mock(SQLMediaLogSproc.class);
        LcmProcessLogDao lcmProcessLogDao = new LcmProcessLogDao(mockMediaProcessLogSproc);
        String searchName = "1037678_109010ice.jpg";
        List<String> fileList = new ArrayList<>();
        fileList.add(searchName);
        Map<String, Object> value = new HashMap<String, Object>();
        List<MediaProcessLog>
                mediaLogStatuses = new ArrayList<MediaProcessLog>();

        value.put(SQLMediaLogSproc.MEDIAS_RESULT_SET, mediaLogStatuses);
        when(mockMediaProcessLogSproc.execute(searchName)).thenReturn(value);
        List<MediaProcessLog> mediaLogStatuses1 = lcmProcessLogDao.findMediaStatus(fileList);
        assertEquals(mediaLogStatuses1, null);
    }
}
