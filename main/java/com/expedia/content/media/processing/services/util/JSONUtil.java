package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.dao.MediaProcessLog;
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
    private static final String JSON_TAG_STATUS_NOT_FOUND = "NOT_FOUND";
    private static final Logger LOGGER = LoggerFactory.getLogger(JSONUtil.class);

    private JSONUtil() {
    }

    /**
     * convert json message to java map object
     *
     * @param jsonMessage
     * @return Map
     * @throws RequestMessageException happens when message is invalid json format.
     */
    public static Map buildMapFromJson(String jsonMessage) throws RequestMessageException {
        try {
            return OBJECT_MAPPER.readValue(jsonMessage, Map.class);
        } catch (IOException ex) {
            String errorMsg = MessageFormat.format("Error parsing/converting Json message: {0}", jsonMessage);
            throw new RequestMessageException(errorMsg, ex);
        }
    }

    /**
     * Generate the json response message.
     *
     * @param mapList        key is media file name, value is status list that get from LCM DB MediaProcessLog.
     * @param fileNameList   media file name list from input json message
     * @param mediaStatusMap activityType to status messsage mapping.
     * @return
     * @throws Exception happen when covert map to json error.
     */
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
                String mappingValue = null;
                for (int i = eachList.size() - 1; i >= 0; i--) {
                    MediaProcessLog mediaProcessLog = eachList.get(i);
                    LOGGER.debug("status type from db:" + mediaProcessLog.getActivityType());
                    mappingValue =
                            mediaStatusMap.get(mediaProcessLog.getActivityType());
                    if (mappingValue != null) {
                        eachEntryMap.put(JSON_TAG_STATUS, mappingValue);
                        eachEntryMap.put(JSON_TAG_TIME, mediaProcessLog.getActivityTime());
                        break;
                    } else {
                        continue;
                    }
                }
                if (mappingValue == null) {
                    eachEntryMap.put(JSON_TAG_STATUS, JSON_TAG_STATUS_NOT_FOUND);
                }

            } else {
                eachEntryMap.put(JSON_TAG_STATUS, JSON_TAG_STATUS_NOT_FOUND);
            }
            mediaStatusList.add(eachEntryMap);
        }
        allMap.put(JSON_TAG_MEDIA_STATUS, mediaStatusList);
        return mapper.writeValueAsString(allMap);
    }

    /**
     * convert all media status list to mediaFileName-statusList mapping.
     *
     * @param statusLogList all media status of all input media files, and get from LCM DB.
     * @param mapList       key is media file name, value is status list that get from LCM DB MediaProcessLog.
     * @param size          the input media file name list size.
     */
    public static void divideListToMap(List<MediaProcessLog> statusLogList, Map<String, List<MediaProcessLog>> mapList, int size) {
        if (statusLogList != null && statusLogList.size() > 0) {
            List[] sublist = new ArrayList[size];
            for (int k = 0; k < size; k++) {
                List<MediaProcessLog> eachNameList = new ArrayList<>();
                sublist[k] = eachNameList;
            }
            int i = 0;
            String preName = statusLogList.get(0).getMediaFileName();
            mapList.put(preName, sublist[0]);
            for (MediaProcessLog mediaProcessLog : statusLogList) {
                if (mediaProcessLog.getMediaFileName().equals(preName)) {
                    sublist[i].add(mediaProcessLog);
                } else {
                    i++;
                    sublist[i].add(mediaProcessLog);
                    preName = mediaProcessLog.getMediaFileName();
                    mapList.put(preName, sublist[i]);
                }
            }
        }
    }

}
