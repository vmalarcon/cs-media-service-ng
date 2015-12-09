package com.expedia.content.media.processing.services.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.MediaProcessLog;

import java.util.*;

import org.junit.Test;

public class JSONUtilTest {

    @Test
    public void testbuildMapFromJson() throws Exception {
        String jsonString = "{  \n"
                + "   \"mediaNames\":\"1037678_109010ice.jpg\"\n"
                + "}";
        String value = (String) JSONUtil.buildMapFromJson(jsonString).get("mediaNames");
        assertTrue(("1037678_109010ice.jpg").equals(value));
    }

    @Test
    public void testGenerateJsonResponse() throws Exception {
        String expededJson =
                "{\"mediaStatuses\":[{\"mediaName\":\"test.jpg\",\"status\":\"PUBLISHED\",\"time\":\"2014-07-11 16:25:06.0290552 -07:00\"}]}";
        MediaProcessLog mediaProcessLog = new MediaProcessLog("2014-07-11 16:25:06.0290552 -07:00", "test.jpg", "Publish", "Lodging");

        List<MediaProcessLog> mediaProcessLogs = new ArrayList<>();
        mediaProcessLogs.add(mediaProcessLog);

        Map<String, java.util.List<MediaProcessLog>> mapList = new HashMap<>();
        mapList.put("test.jpg", mediaProcessLogs);

        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("test.jpg");
        Map<String, String> mediaStatusMap = new HashMap<>();
        mediaStatusMap.put("Publish", "PUBLISHED");
        ActivityMapping activityMapping = new ActivityMapping();
        activityMapping.setActivityType("Publish");
        activityMapping.setMediaType(".*");
        activityMapping.setStatusMessage("PUBLISHED");
        List<ActivityMapping> activityMappingList = new ArrayList<>();
        activityMappingList.add(activityMapping);

        String response = JSONUtil.generateJsonByProcessLogList(mapList, fileNameList, activityMappingList);
        assertTrue(expededJson.equals(response));
    }

    @Test
    public void testGenerateBadRequestResponse() throws Exception {
        String res = JSONUtil.generateJsonForErrorResponse("bad request", "/testurl", 400, "field is required");
        Map jsonMap = JSONUtil.buildMapFromJson(res);
        assertTrue("bad request".equals(jsonMap.get("message")));
        assertTrue("/testurl".equals(jsonMap.get("path")));
        assertTrue(Integer.parseInt(jsonMap.get("status").toString()) == 400);
        assertTrue("field is required".equals(jsonMap.get("error")));

    }

    @Test
    public void testGenerateJsonResponseCar() throws Exception {
        String expededJson =
                "{\"mediaStatuses\":[{\"mediaName\":\"test.jpg\",\"status\":\"RECEIVED\",\"time\":\"2014-07-11 16:25:06.0290552 -07:00\"}]}";
        MediaProcessLog mediaProcessLog = new MediaProcessLog("2014-07-11 16:25:06.0290552 -07:00", "test.jpg", "DcpPickup", "Cars");

        List<MediaProcessLog> mediaProcessLogs = new ArrayList<>();
        mediaProcessLogs.add(mediaProcessLog);

        Map<String, java.util.List<MediaProcessLog>> mapList = new HashMap<>();
        mapList.put("test.jpg", mediaProcessLogs);

        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("test.jpg");
        ActivityMapping activityMapping = new ActivityMapping();
        activityMapping.setActivityType("DcpPickup");
        activityMapping.setMediaType("Cars");
        activityMapping.setStatusMessage("RECEIVED");
        List<ActivityMapping> activityMappingList = new ArrayList<>();
        activityMappingList.add(activityMapping);

        String response = JSONUtil.generateJsonByProcessLogList(mapList, fileNameList, activityMappingList);
        assertTrue(expededJson.equals(response));
    }

    @Test
    public void testGenerateJsonResponseNotFound() throws Exception {
        String expededJson =
                "{\"mediaStatuses\":[{\"mediaName\":\"test.jpg\",\"status\":\"NOT_FOUND\"}]}";
        MediaProcessLog mediaProcessLog = new MediaProcessLog("2014-07-11 16:25:06.0290552 -07:00", "test.jpg", "UnKnown", "Lodging");

        List<MediaProcessLog> mediaProcessLogs = new ArrayList<>();
        mediaProcessLogs.add(mediaProcessLog);

        Map<String, java.util.List<MediaProcessLog>> mapList = new HashMap<>();
        mapList.put("test.jpg", mediaProcessLogs);

        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("test.jpg");
        Map<String, String> mediaStatusMap = new HashMap<>();
        mediaStatusMap.put("Publish", "PUBLISHED");
        ActivityMapping activityMapping = new ActivityMapping();
        activityMapping.setActivityType("Reception");
        activityMapping.setMediaType("*");
        activityMapping.setStatusMessage("PUBLISHED");
        List<ActivityMapping> activityMappingList = new ArrayList<>();
        activityMappingList.add(activityMapping);

        String response = JSONUtil.generateJsonByProcessLogList(mapList, fileNameList, activityMappingList);
        assertTrue(expededJson.equals(response));
    }

    @Test
    public void testDivideListToMap() throws Exception {
        String expededJson =
                "{\"mediaStatuses\":[{\"mediaName\":\"test.jpg\",\"status\":\"PUBLISHED\",\"time\":\"2014-07-11 16:25:06.0290552 -07:00\"}]}";
        MediaProcessLog mediaProcessLog1 = new MediaProcessLog("2014-07-11 16:24:06.0290552 -07:00", "test1.jpg", "Publish", "Lodging");
        MediaProcessLog mediaProcessLog2 = new MediaProcessLog("2014-07-11 16:24:07.0290552 -07:00", "tEst1.jpg", "Publish", "Lodging");
        MediaProcessLog mediaProcessLog3 = new MediaProcessLog("2014-07-11 16:25:06.0290552 -07:00", "test1.jpg", "Publish", "Lodging");

        MediaProcessLog mediaProcessLog4 = new MediaProcessLog("2014-07-11 16:26:06.0290552 -07:00", "test2.jpg", "Publish", "Lodging");
        MediaProcessLog mediaProcessLog5 = new MediaProcessLog("2014-07-11 16:27:06.0290552 -07:00", "test2.jpg", "Publish", "Lodging");
        MediaProcessLog mediaProcessLog6 = new MediaProcessLog("2014-07-11 16:28:06.0290552 -07:00", "test2.jpg", "Publish", "Lodging");

        List<MediaProcessLog> statusLogList = new ArrayList<>();
        statusLogList.add(mediaProcessLog1);
        statusLogList.add(mediaProcessLog2);
        statusLogList.add(mediaProcessLog3);
        statusLogList.add(mediaProcessLog4);
        statusLogList.add(mediaProcessLog5);
        statusLogList.add(mediaProcessLog6);

        Map<String, java.util.List<MediaProcessLog>> mapList = new HashMap<>();
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("test1.jpg");
        fileNameList.add("test2.jpg");
        JSONUtil.divideStatusListToMap(statusLogList, mapList, fileNameList.size());
        assertTrue(("2014-07-11 16:24:06.0290552 -07:00").equals(mapList.get("test1.jpg").get(0).getActivityTime()));
        assertTrue(("2014-07-11 16:26:06.0290552 -07:00").equals(mapList.get("test2.jpg").get(0).getActivityTime()));

    }

    @Test
    public void testListToJsonString() throws Exception {
        String expectedJson =
                "[{\"fileName\":\"test1.jpg\",\"error\":\"soureceid is missed\"},{\"fileName2\":\"test2.jpg\",\"error\":\"expediaId is missed\"}]";
        List<Map<String, String>> messageList = new ArrayList<>();
        Map<String, String> map1 = new LinkedHashMap<>();
        map1.put("fileName", "test1.jpg");
        map1.put("error", "soureceid is missed");
        Map<String, String> map2 = new LinkedHashMap<>();
        map2.put("fileName2", "test2.jpg");
        map2.put("error", "expediaId is missed");

        messageList.add(map1);
        messageList.add(map2);
        assertTrue(expectedJson.equals(JSONUtil.convertValidationErrors(messageList)));
    }

    @Test
    public void testConvertToCommonMessage() throws Exception {
        String message = "{ \n"
                + "   \"mediaProviderId\":\"1\",\n"
                + "     \"fileUrl\":\"http://localhost:38081/office.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n"
                + "   \"stagingKey\":{ \n"
                + "      \"externalId\":\"222\",\n"
                + "      \"providerId\":\"300\",\n"
                + "      \"sourceId\":\"99\"\n"
                + "   },\n"
                + "   \"expediaId\":429,\n"
                + "   \"categoryId\":\"801\",\n"
                + "   \"callback\":\"http://multi.source.callback/callback\",\n"
                + "   \"caption\":\"caption\"\n"
                + "}";

        String convert = "{\"fileUrl\":\"http:\\/\\/localhost:38081\\/office.jpg\",\"fileName\":\"429_1_office.jpg\",\"domain\":\"Lodging\",\"callback\":\"http:\\/\\/multi.source.callback\\/callback\",\"active\":\"true\",\"stagingKey\":{\"externalId\":\"222\",\"providerId\":\"300\",\"sourceId\":\"99\"},\"domainId\":\"429\",\"domainFields\":{\"category\":\"801\"},\"caption\":\"caption\",\"domainProvider\":\"EPC Internal User\",\"userId\":\"MultiSource\",\"clientId\":\"MultiSource\"}";

        ImageMessage imageMessageOld = ImageMessage.parseJsonMessage(message);
        Map messageMap = JSONUtil.buildMapFromJson(message);
        Properties properties = new Properties();
        properties.put("1","EPC Internal User");

        String mediaCommonMessage = JSONUtil.convertToCommonMessage(imageMessageOld, messageMap, properties);
        assertEquals(convert, mediaCommonMessage);
    }
}
