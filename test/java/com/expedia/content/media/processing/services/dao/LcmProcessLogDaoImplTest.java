package com.expedia.content.media.processing.services.dao;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LcmProcessLogDaoImplTest {

    @Test
    public void testMediaStatusFound() {
        String processName = "lcm-reporting-test";
        SQLMediaLogSproc mockMediaProcessLogSproc = mock(SQLMediaLogSproc.class);
        LcmProcessLogDaoImpl lcmProcessLogDao = new LcmProcessLogDaoImpl(processName, mockMediaProcessLogSproc);
        String searchName = "1037678_109010ice.jpg";
        List<String> fileList = new ArrayList<>();
        fileList.add(searchName);
        Map<String, Object> value = new HashMap<String, Object>();
        List<MediaProcessLog>
                mediaLogStatuses = new ArrayList<MediaProcessLog>();
        MediaProcessLog
                mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "LCM/Publish", "1037678_109010ice.jpg");
        mediaLogStatuses.add(mediaLogStatus);
        value.put(SQLMediaLogSproc.MEDIAS_RESULT_SET, mediaLogStatuses);
        when(mockMediaProcessLogSproc.execute(searchName)).thenReturn(value);
        List<MediaProcessLog> mediaLogStatuses1 = lcmProcessLogDao.findMediaStatus(fileList);
        assertEquals(mediaLogStatuses1.get(0), mediaLogStatuses.get(0));
    }

    @Test
    public void testMediaStatusNotFound() {
        String processName = "lcm-reporting-test";
        SQLMediaLogSproc mockMediaProcessLogSproc = mock(SQLMediaLogSproc.class);
        LcmProcessLogDaoImpl lcmProcessLogDao = new LcmProcessLogDaoImpl(processName, mockMediaProcessLogSproc);
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
