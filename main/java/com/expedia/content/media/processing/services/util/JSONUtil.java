package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.pipleline.reporting.sql.MediaProcessLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * contain method to process json request and generate json response.
 */
public final class JSONUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String JSON_TAG_STATUS = "status";
    private static final String JSON_TAG_TIME = "time";
    private static final String JSON_TAG_MEDIA_NAME = "mediaName";
    private static final String JSON_TAG_MEDIA_STATUS = "mediaStatuses";
    private static final String JSON_TAG_STATUS_LIST = "statuses";
    private static final String JSON_TAG_STATUS_NOT_FOUND = "status is not found";
    private static final Logger LOGGER = LoggerFactory.getLogger(JSONUtil.class);

    private JSONUtil() {
    }

    public static Map buildMapFromJson(String jsonMessage) throws ImageStatusException {
        try {
            return OBJECT_MAPPER.readValue(jsonMessage, Map.class);
        } catch (IOException ex) {
            String errorMsg = MessageFormat.format("Error parsing/converting Json message: {0}", jsonMessage);
            throw new ImageStatusException(errorMsg, ex);
        }
    }

    public static String generateJsonResponse(Map<String, List<MediaProcessLog>> mapList, List<String> fileNameList, Map<String, String> mediaStatusMap)
            throws Exception {
        Map<String, Object> allMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        List mediaStatusList = new ArrayList();
        for (String fileName : fileNameList) {
            Map<String, Object> eachEntryMap = new LinkedHashMap<>();
            eachEntryMap.put(JSON_TAG_MEDIA_NAME, fileName);

            List<MediaProcessLog> eachList = mapList.get(fileName);
            if (eachList != null && eachList.size() > 0) {
                MediaProcessLog mediaProcessLog = eachList.get(eachList.size() - 1);
                LOGGER.debug("status type from db:" + mediaProcessLog.getActivityType());
                String mappingValue =
                        mediaStatusMap.get(mediaProcessLog.getActivityType()) != null ? mediaStatusMap.get(mediaProcessLog.getActivityType()) :
                                "Unrecognized status:" + mediaProcessLog.getActivityType();
                eachEntryMap.put(JSON_TAG_STATUS, mappingValue);
                eachEntryMap.put(JSON_TAG_TIME, mediaProcessLog.getActivityTime());
            } else {
                eachEntryMap.put(JSON_TAG_STATUS, JSON_TAG_STATUS_NOT_FOUND);
            }
            mediaStatusList.add(eachEntryMap);
        }
        allMap.put(JSON_TAG_MEDIA_STATUS, mediaStatusList);
        return mapper.writeValueAsString(allMap);
    }

}
