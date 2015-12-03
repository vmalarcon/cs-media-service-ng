package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.MessageConstants;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.MediaProcessLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.tools.json.JSONWriter;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.UUID;

/**
 * Contains methods to process JSON requests and generate JSON responses.
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
     * Converts json message to java map object
     *
     * @param jsonMessage input json format message
     * @return Map map object that contain json message value.
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

    public static List<Map> buildMapListFromJson(String jsonMessage) throws RequestMessageException {
        try {
            return OBJECT_MAPPER.readValue(jsonMessage, List.class);
        } catch (IOException ex) {
            String errorMsg = MessageFormat.format("Error parsing/converting Json message: {0}", jsonMessage);
            throw new RequestMessageException(errorMsg, ex);
        }
    }

    /**
     * convert the imageMessage validation error list to JSON string
     *
     * @param messageList map message with attribute fileName and error
     * @return JSON string contains fileName and error description
     */
    public static String convertValidationErrors(List<Map<String, String>> messageList) {
        try {
            return OBJECT_MAPPER.writeValueAsString(messageList);
        } catch (IOException ex) {
            String errorMsg = "Error writing map to json";
            throw new RequestMessageException(errorMsg, ex);
        }
    }

    /**
     * Generate the json response message.
     *
     * @param mapList             key is media file name, value is status list that get from DB MediaProcessLog.
     * @param fileNameList        media file name list from input json message
     * @param activityMappingList activityType to status messsage mapping.
     * @return
     * @throws RequestMessageException happen when covert map to json error.
     */
    public static String generateJsonByProcessLogList(Map<String, List<MediaProcessLog>> mapList, List<String> fileNameList,
            List<ActivityMapping> activityMappingList)
            throws RequestMessageException {
        Map<String, Object> allMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        List mediaStatusList = new ArrayList();
        for (String fileName : fileNameList) {
            Map<String, Object> eachEntryMap = new LinkedHashMap<>();
            eachEntryMap.put(JSON_TAG_MEDIA_NAME, fileName);

            List<MediaProcessLog> eachList = mapList.get(fileName);
            if (eachList != null && eachList.size() > 0) {
                ActivityMapping activityMapping = null;
                for (int i = eachList.size() - 1; i >= 0; i--) {
                    MediaProcessLog mediaProcessLog = eachList.get(i);
                    activityMapping = getMappingFromList(activityMappingList, mediaProcessLog.getActivityType(), mediaProcessLog.getMediaType());
                    if (activityMapping != null) {
                        eachEntryMap.put(JSON_TAG_STATUS, activityMapping.getStatusMessage());
                        eachEntryMap.put(JSON_TAG_TIME, mediaProcessLog.getActivityTime());
                        break;
                    } else {
                        continue;
                    }
                }
                if (activityMapping == null) {
                    eachEntryMap.put(JSON_TAG_STATUS, JSON_TAG_STATUS_NOT_FOUND);
                }

            } else {
                eachEntryMap.put(JSON_TAG_STATUS, JSON_TAG_STATUS_NOT_FOUND);
            }
            mediaStatusList.add(eachEntryMap);
        }
        allMap.put(JSON_TAG_MEDIA_STATUS, mediaStatusList);
        try {
            return mapper.writeValueAsString(allMap);
        } catch (IOException ex) {
            String errorMsg = "Error writing map to json";
            throw new RequestMessageException(errorMsg, ex);
        }

    }

    /**
     * generate json format error message
     *
     * @param message detail error message
     * @param urlPath web service url
     * @param status  http status
     * @param error
     * @return json format error message
     */
    public static String generateJsonForErrorResponse(String message, String urlPath, int status, String error) {
        Map<String, Object> allMap = new TreeMap<>();
        ObjectMapper mapper = new ObjectMapper();
        allMap.put("path", urlPath);
        allMap.put("message", message);
        allMap.put("error", error);
        allMap.put("status", status);
        allMap.put("timestamp", new Date().getTime());
        try {
            return mapper.writeValueAsString(allMap);
        } catch (IOException ex) {
            String errorMsg = "Error writing map to json";
            throw new RequestMessageException(errorMsg, ex);
        }
    }

    private static ActivityMapping getMappingFromList(List<ActivityMapping> activityMappingList, String activityType, String mediaType) {
        for (ActivityMapping activityMapping : activityMappingList) {
            if (activityMapping.getActivityType().equals(activityType) && mediaType.matches(activityMapping.getMediaType())) {
                return activityMapping;
            }
        }
        return null;
    }

    /**
     * convert all media status list to mediaFileName-statusList mapping.
     *
     * @param statusLogList all media status of all input media files, and get from LCM DB.
     * @param mapList       key is media file name, value is status list that get from LCM DB MediaProcessLog.
     * @param size          the input media file name list size.
     */
    public static void divideStatusListToMap(List<MediaProcessLog> statusLogList, Map<String, List<MediaProcessLog>> mapList, int size) {
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
                if (mediaProcessLog.getMediaFileName().equalsIgnoreCase(preName)) {
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

    public static void addGuidToMap(Map messageMap) {
        if (messageMap.get("mediaGuid") == null) {
            final String guid = UUID.randomUUID().toString();
            messageMap.put("mediaGuid", guid);
        }
    }

    /**
     * Builds the old JSON message of this image message.
     *
     * @return The ImageMessage as a JSON message.
     */
    public static String convertToCommonMessage(ImageMessage imageMessage, Map map,Properties providerProperties) {
        Map<String, Object> mapMessage = new LinkedHashMap<>();
        Map<String, Object> domainMapMessage = new LinkedHashMap<>();

        if (imageMessage.getFileUrl() != null) {
            mapMessage.put(MessageConstants.FILE_URL, imageMessage.getFileUrl());
        }
        if (imageMessage.getFileName() != null) {
            mapMessage.put(MessageConstants.FILE_NAME, imageMessage.getFileName());
        } else {
            String fileName = "";
            if(imageMessage.getFileUrl() != null){
                fileName = FilenameUtils.getBaseName(imageMessage.getFileUrl())+".jpg";
            }
            mapMessage.put(MessageConstants.FILE_NAME, fileName);
        }
        if (map.get("imageType") != null) {
            mapMessage.put("domain", map.get("imageType"));
        }
        if (imageMessage.getCallback() != null) {
            mapMessage.put(MessageConstants.CALLBACK, imageMessage.getCallback().toString());
        }
        if (map.get("mediaGuid") != null) {
            mapMessage.put(MessageConstants.MEDIA_ID, map.get("mediaGuid"));
        }
        if (imageMessage.getStagingKey() != null) {
            mapMessage.put(MessageConstants.STAGING_KEY, imageMessage.getStagingKey());
        }
        if(imageMessage.getExpediaId()!=null){
            mapMessage.put(MessageConstants.OUTER_DOMAIN_ID, imageMessage.getExpediaId().toString());
        }
        if (imageMessage.getCategoryId() != null) {
            domainMapMessage.put("category",imageMessage.getCategoryId());
            mapMessage.put(MessageConstants.OUTER_DOMAIN_FIELDS, domainMapMessage);
        }
        if (imageMessage.getCaption() != null) {
            mapMessage.put(MessageConstants.CAPTION, imageMessage.getCaption());
        }
        if (imageMessage.getMediaProviderId()!=null) {
            mapMessage.put(MessageConstants.OUTER_DOMAIN_PROVIDER,
                    providerProperties.getProperty(imageMessage.getMediaProviderId()));
        }
        mapMessage.put(MessageConstants.USER_ID,
                "MultiSource");
        mapMessage.put(MessageConstants.CLIENT_ID,
                "MultiSource");
        return new JSONWriter().write(mapMessage);
    }

}
