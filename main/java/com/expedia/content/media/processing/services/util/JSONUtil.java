package com.expedia.content.media.processing.services.util;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;

import com.expedia.content.media.processing.services.dao.Category;
import org.apache.commons.io.FilenameUtils;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.MessageConstants;
import com.expedia.content.media.processing.services.dao.MediaProcessLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.tools.json.JSONWriter;

/**
 * Contains methods to process JSON requests and generate JSON responses.
 */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.ModifiedCyclomaticComplexity", "PMD.StdCyclomaticComplexity", "PMD.ConfusingTernary", "PMD.NPathComplexity"})
public final class JSONUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String JSON_TAG_STATUS = "status";
    private static final String JSON_TAG_TIME = "time";
    private static final String JSON_TAG_MEDIA_NAME = "mediaName";
    private static final String JSON_TAG_MEDIA_STATUS = "mediaStatuses";
    private static final String JSON_TAG_STATUS_NOT_FOUND = "NOT_FOUND";
    private static final String JSON_TAG_DOMAIN = "domain";
    private static final String ERROR_WRITING_MAP = "Error writing map to json";
    private static final String MEDIA_GUID = "mediaGuid";

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
            final String errorMsg = MessageFormat.format("Error parsing/converting Json message: {0}", jsonMessage);
            throw new RequestMessageException(errorMsg, ex);
        }
    }

    public static List<Map> buildMapListFromJson(String jsonMessage) throws RequestMessageException {
        try {
            return OBJECT_MAPPER.readValue(jsonMessage, List.class);
        } catch (IOException ex) {
            final String errorMsg = MessageFormat.format("Error parsing/converting Json message: {0}", jsonMessage);
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
            final String errorMsg = ERROR_WRITING_MAP;
            throw new RequestMessageException(errorMsg, ex);
        }
    }

    /**
     * Generate the json response message.
     *
     * @param categories    a List of Categories with their Sub-Categories
     * @param domain        The domain of the categories
     * @return
     */
    public static String generateJsonByCategoryList(List<Category> categories, String domain) {
        try {
            Map<String, Object> allMap = new HashMap<>();
            allMap.put(JSON_TAG_DOMAIN, domain);
            allMap.put("categories", categories);
            return OBJECT_MAPPER.writeValueAsString(allMap);
        } catch (IOException ex) {
            String errorMsg = ERROR_WRITING_MAP;
            throw new RequestMessageException(errorMsg, ex);
        }
    }

    /**
     * Generate the json response message.
     *
     * @param mapList key is media file name, value is status list that get from DB MediaProcessLog.
     * @param fileNameList media file name list from input json message
     * @param activityMappingList activityType to status messsage mapping.
     * @return
     * @throws RequestMessageException happen when covert map to json error.
     */
    public static String generateJsonByProcessLogList(Map<String, List<MediaProcessLog>> mapList, List<String> fileNameList,
                                                      List<ActivityMapping> activityMappingList) throws RequestMessageException {
        final Map<String, Object> allMap = new HashMap<>();
        final ObjectMapper mapper = new ObjectMapper();
        final List mediaStatusList = new ArrayList();
        for (final String fileName : fileNameList) {
            final Map<String, Object> eachEntryMap = new LinkedHashMap<>();
            eachEntryMap.put(JSON_TAG_MEDIA_NAME, fileName);

            final List<MediaProcessLog> eachList = mapList.get(fileName);
            if (eachList != null && !eachList.isEmpty()) {
                ActivityMapping activityMapping = null;
                for (int i = eachList.size() - 1; i >= 0; i--) {
                    final MediaProcessLog mediaProcessLog = eachList.get(i);
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
            final String errorMsg = ERROR_WRITING_MAP;
            throw new RequestMessageException(errorMsg, ex);
        }

    }

    /**
     * generate json format error message
     *
     * @param message detail error message
     * @param urlPath web service url
     * @param status http status
     * @param error
     * @return json format error message
     */
    public static String generateJsonForErrorResponse(String message, String urlPath, int status, String error) {
        final Map<String, Object> allMap = new TreeMap<>();
        final ObjectMapper mapper = new ObjectMapper();
        allMap.put("path", urlPath);
        allMap.put("message", message);
        allMap.put("error", error);
        allMap.put("status", status);
        allMap.put("timestamp", new Date().getTime());
        try {
            return mapper.writeValueAsString(allMap);
        } catch (IOException ex) {
            final String errorMsg = ERROR_WRITING_MAP;
            throw new RequestMessageException(errorMsg, ex);
        }
    }

    private static ActivityMapping getMappingFromList(List<ActivityMapping> activityMappingList, String activityType, String mediaType) {
        for (final ActivityMapping activityMapping : activityMappingList) {
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
     * @param mapList key is media file name, value is status list that get from LCM DB MediaProcessLog.
     * @param size the input media file name list size.
     */
    public static void divideStatusListToMap(List<MediaProcessLog> statusLogList, Map<String, List<MediaProcessLog>> mapList, int size) {
        if (statusLogList != null && !statusLogList.isEmpty()) {
            List[] sublist = new ArrayList[size];
            for (int k = 0; k < size; k++) {
                final List<MediaProcessLog> eachNameList = new ArrayList<>();
                sublist[k] = eachNameList;
            }
            int i = 0;
            String preName = statusLogList.get(0).getMediaFileName();
            mapList.put(preName, sublist[0]);
            for (final MediaProcessLog mediaProcessLog : statusLogList) {
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
        if (messageMap.get(MEDIA_GUID) == null) {
            final String guid = UUID.randomUUID().toString();
            messageMap.put(MEDIA_GUID, guid);
        }
    }

    /**
     * Builds the old JSON message of this image message.
     *
     * @return The ImageMessage as a JSON message.
     */
    public static String convertToCommonMessage(ImageMessage imageMessage, Map map, Properties providerProperties) {
        final Map<String, Object> mapMessage = new LinkedHashMap<>();
        final Map<String, Object> domainMapMessage = new LinkedHashMap<>();

        if (imageMessage.getFileUrl() != null) {
            mapMessage.put(MessageConstants.FILE_URL, imageMessage.getFileUrl());
        }
        if (imageMessage.getFileName() != null) {
            mapMessage.put(MessageConstants.FILE_NAME, imageMessage.getFileName());
        } else {
            String fileName = "";
            if (imageMessage.getFileUrl() != null) {
                fileName = ((imageMessage.getExpediaId() == null) ? "" : imageMessage.getExpediaId() + "_") + ((imageMessage.getMediaProviderId() == null) ? "" :
                        imageMessage.getMediaProviderId() + "_") + FilenameUtils.getBaseName(imageMessage.getFileUrl()) + ".jpg";
            }
            mapMessage.put(MessageConstants.FILE_NAME, fileName);
        }
        if (map.get("imageType") != null) {
            mapMessage.put("domain", map.get("imageType"));
        }
        if (imageMessage.getCallback() != null) {
            mapMessage.put(MessageConstants.CALLBACK, imageMessage.getCallback().toString());
        }
        // set default to true if "active" is not set.
        if (map.get("active") == null) {
            mapMessage.put(MessageConstants.ACTIVE, "true");
        } else {
            mapMessage.put(MessageConstants.ACTIVE, map.get("active"));
        }
        if (map.get(MEDIA_GUID) != null) {
            mapMessage.put(MessageConstants.MEDIA_ID, map.get(MEDIA_GUID));
        }
        if (imageMessage.getStagingKey() != null) {
            mapMessage.put(MessageConstants.STAGING_KEY, imageMessage.getStagingKey());
        }
        if (imageMessage.getExpediaId() != null) {
            mapMessage.put(MessageConstants.OUTER_DOMAIN_ID, imageMessage.getExpediaId().toString());
        }
        if (imageMessage.getCategoryId() != null) {
            domainMapMessage.put("category", imageMessage.getCategoryId());
            mapMessage.put(MessageConstants.OUTER_DOMAIN_FIELDS, domainMapMessage);
        }
        if (imageMessage.getCaption() != null) {
            mapMessage.put(MessageConstants.CAPTION, imageMessage.getCaption());
        }
        if (imageMessage.getMediaProviderId() != null) {
            mapMessage.put(MessageConstants.OUTER_DOMAIN_PROVIDER, providerProperties.getProperty(imageMessage.getMediaProviderId()));
        }
        mapMessage.put(MessageConstants.USER_ID, "MultiSource");
        mapMessage.put(MessageConstants.CLIENT_ID, "MultiSource");
        return new JSONWriter().write(mapMessage);
    }

}
