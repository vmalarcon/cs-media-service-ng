package com.expedia.content.media.processing.services.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;

import java.util.*;

import org.junit.Test;

public class JSONUtilTest {
    
    @Test
    public void testbuildMapFromJson() throws Exception {
        final String jsonString = "{  \n"
                + "   \"mediaNames\":\"1037678_109010ice.jpg\"\n"
                + "}";
        final String value = (String) JSONUtil.buildMapFromJson(jsonString).get("mediaNames");
        assertTrue("1037678_109010ice.jpg".equals(value));
    }
    
    @Test
    public void testGenerateJsonResponse() throws Exception {
        final String expededJson =
                "{\"mediaStatuses\":[{\"mediaName\":\"test.jpg\",\"status\":\"PUBLISHED\",\"time\":\"2014-07-11 16:25:06.0290552 -07:00\"}]}";
        final MediaProcessLog mediaProcessLog = new MediaProcessLog("2014-07-11 16:25:06.0290552 -07:00", "test.jpg", "Publish", "Lodging");
        
        final List<MediaProcessLog> mediaProcessLogs = new ArrayList<>();
        mediaProcessLogs.add(mediaProcessLog);
        
        final Map<String, List<MediaProcessLog>> mapList = new HashMap<>();
        mapList.put("test.jpg", mediaProcessLogs);
        
        final List<String> fileNameList = new ArrayList<>();
        fileNameList.add("test.jpg");
        final Map<String, String> mediaStatusMap = new HashMap<>();
        mediaStatusMap.put("Publish", "PUBLISHED");
        final ActivityMapping activityMapping = new ActivityMapping();
        activityMapping.setActivityType("Publish");
        activityMapping.setMediaType(".*");
        activityMapping.setStatusMessage("PUBLISHED");
        final List<ActivityMapping> activityMappingList = new ArrayList<>();
        activityMappingList.add(activityMapping);
        
        final String response = JSONUtil.generateJsonByProcessLogList(mapList, fileNameList, activityMappingList);
        assertTrue(expededJson.equals(response));
    }
    
    @Test
    public void testGenerateBadRequestResponse() throws Exception {
        final String res = JSONUtil.generateJsonForErrorResponse("bad request", "/testurl", 400, "field is required");
        Map jsonMap = JSONUtil.buildMapFromJson(res);
        assertTrue("bad request".equals(jsonMap.get("message")));
        assertTrue("/testurl".equals(jsonMap.get("path")));
        assertTrue(Integer.parseInt(jsonMap.get("status").toString()) == 400);
        assertTrue("field is required".equals(jsonMap.get("error")));
        
    }
    
    @Test
    public void testGenerateJsonResponseCar() throws Exception {
        final String expededJson =
                "{\"mediaStatuses\":[{\"mediaName\":\"test.jpg\",\"status\":\"RECEIVED\",\"time\":\"2014-07-11 16:25:06.0290552 -07:00\"}]}";
        final MediaProcessLog mediaProcessLog = new MediaProcessLog("2014-07-11 16:25:06.0290552 -07:00", "test.jpg", "DcpPickup", "Cars");
        
        final List<MediaProcessLog> mediaProcessLogs = new ArrayList<>();
        mediaProcessLogs.add(mediaProcessLog);
        
        final Map<String, List<MediaProcessLog>> mapList = new HashMap<>();
        mapList.put("test.jpg", mediaProcessLogs);
        
        final List<String> fileNameList = new ArrayList<>();
        fileNameList.add("test.jpg");
        final ActivityMapping activityMapping = new ActivityMapping();
        activityMapping.setActivityType("DcpPickup");
        activityMapping.setMediaType("Cars");
        activityMapping.setStatusMessage("RECEIVED");
        final List<ActivityMapping> activityMappingList = new ArrayList<>();
        activityMappingList.add(activityMapping);
        
        final String response = JSONUtil.generateJsonByProcessLogList(mapList, fileNameList, activityMappingList);
        assertTrue(expededJson.equals(response));
    }
    
    @Test
    public void testGenerateJsonResponseNotFound() throws Exception {
        final String expededJson =
                "{\"mediaStatuses\":[{\"mediaName\":\"test.jpg\",\"status\":\"NOT_FOUND\"}]}";
        final MediaProcessLog mediaProcessLog = new MediaProcessLog("2014-07-11 16:25:06.0290552 -07:00", "test.jpg", "UnKnown", "Lodging");
        
        final List<MediaProcessLog> mediaProcessLogs = new ArrayList<>();
        mediaProcessLogs.add(mediaProcessLog);
        
        final Map<String, List<MediaProcessLog>> mapList = new HashMap<>();
        mapList.put("test.jpg", mediaProcessLogs);
        
        final List<String> fileNameList = new ArrayList<>();
        fileNameList.add("test.jpg");
        final Map<String, String> mediaStatusMap = new HashMap<>();
        mediaStatusMap.put("Publish", "PUBLISHED");
        final ActivityMapping activityMapping = new ActivityMapping();
        activityMapping.setActivityType("Reception");
        activityMapping.setMediaType("*");
        activityMapping.setStatusMessage("PUBLISHED");
        final List<ActivityMapping> activityMappingList = new ArrayList<>();
        activityMappingList.add(activityMapping);
        
        final String response = JSONUtil.generateJsonByProcessLogList(mapList, fileNameList, activityMappingList);
        assertTrue(expededJson.equals(response));
    }
    
    @Test
    public void testDivideListToMap() throws Exception {
        final String expededJson =
                "{\"mediaStatuses\":[{\"mediaName\":\"test.jpg\",\"status\":\"PUBLISHED\",\"time\":\"2014-07-11 16:25:06.0290552 -07:00\"}]}";
        final MediaProcessLog mediaProcessLog1 = new MediaProcessLog("2014-07-11 16:24:06.0290552 -07:00", "test1.jpg", "Publish", "Lodging");
        final MediaProcessLog mediaProcessLog2 = new MediaProcessLog("2014-07-11 16:24:07.0290552 -07:00", "tEst1.jpg", "Publish", "Lodging");
        final MediaProcessLog mediaProcessLog3 = new MediaProcessLog("2014-07-11 16:25:06.0290552 -07:00", "test1.jpg", "Publish", "Lodging");
        
        final MediaProcessLog mediaProcessLog4 = new MediaProcessLog("2014-07-11 16:26:06.0290552 -07:00", "test2.jpg", "Publish", "Lodging");
        final MediaProcessLog mediaProcessLog5 = new MediaProcessLog("2014-07-11 16:27:06.0290552 -07:00", "test2.jpg", "Publish", "Lodging");
        final MediaProcessLog mediaProcessLog6 = new MediaProcessLog("2014-07-11 16:28:06.0290552 -07:00", "test2.jpg", "Publish", "Lodging");
        
        final List<MediaProcessLog> statusLogList = new ArrayList<>();
        statusLogList.add(mediaProcessLog1);
        statusLogList.add(mediaProcessLog2);
        statusLogList.add(mediaProcessLog3);
        statusLogList.add(mediaProcessLog4);
        statusLogList.add(mediaProcessLog5);
        statusLogList.add(mediaProcessLog6);
        
        final Map<String, List<MediaProcessLog>> mapList = new HashMap<>();
        final List<String> fileNameList = new ArrayList<>();
        fileNameList.add("test1.jpg");
        fileNameList.add("test2.jpg");
        JSONUtil.divideStatusListToMap(statusLogList, mapList, fileNameList.size());
        assertTrue("2014-07-11 16:24:06.0290552 -07:00".equals(mapList.get("test1.jpg").get(0).getActivityTime()));
        assertTrue("2014-07-11 16:26:06.0290552 -07:00".equals(mapList.get("test2.jpg").get(0).getActivityTime()));
        
    }
    
    @Test
    public void testListToJsonString() throws Exception {
        final String expectedJson =
                "[{\"fileName\":\"test1.jpg\",\"error\":\"soureceid is missed\"},{\"fileName2\":\"test2.jpg\",\"error\":\"expediaId is missed\"}]";
        final List<Map<String, String>> messageList = new ArrayList<>();
        final Map<String, String> map1 = new LinkedHashMap<>();
        map1.put("fileName", "test1.jpg");
        map1.put("error", "soureceid is missed");
        final Map<String, String> map2 = new LinkedHashMap<>();
        map2.put("fileName2", "test2.jpg");
        map2.put("error", "expediaId is missed");
        
        messageList.add(map1);
        messageList.add(map2);
        assertTrue(expectedJson.equals(JSONUtil.convertValidationErrors(messageList)));
    }
    
    @Test
    public void testConvertToCommonMessage() throws Exception {
        final String message = "{ \n"
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
                + "   \"caption\":\"caption\",\n"
                + "   \"captionLocaleId\":\"1033\"\n"
                + "}";
                
        final String convert =
                "{\"fileUrl\":\"http:\\/\\/localhost:38081\\/office.jpg\",\"providedName\":\"office.jpg\",\"fileName\":\"429_1_office.jpg\",\"domain\":\"Lodging\",\"callback\":\"http:\\/\\/multi.source.callback\\/callback\",\"active\":\"true\",\"stagingKey\":{\"externalId\":\"222\",\"providerId\":\"300\",\"sourceId\":\"99\"},\"domainId\":\"429\",\"domainFields\":{\"subcategoryId\":\"801\",\"caption\":\"caption\",\"captionLocaleId\":\"1033\"},\"domainProvider\":\"EPC Internal User\",\"userId\":\"Multisource\",\"clientId\":\"Multisource\"}";
                
        final ImageMessage imageMessageOld = ImageMessage.parseJsonMessage(message);
        final Map messageMap = JSONUtil.buildMapFromJson(message);
        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");
        
        final String mediaCommonMessage = JSONUtil.convertToCommonMessage(imageMessageOld, messageMap, properties);
        assertEquals(convert, mediaCommonMessage);
    }
    
    @Test
    public void testConvertToCommonMessageActiveTrue() throws Exception {
        final String message = "{ \n"
                + "   \"mediaProviderId\":\"1\",\n"
                + "     \"fileUrl\":\"http://localhost:38081/office.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n"
                + "   \"stagingKey\":{ \n"
                + "      \"externalId\":\"222\",\n"
                + "      \"providerId\":\"300\",\n"
                + "      \"sourceId\":\"99\"\n"
                + "   },\n"
                + "   \"active\":\"true\",\n"
                + "   \"expediaId\":429,\n"
                + "   \"categoryId\":\"801\",\n"
                + "   \"callback\":\"http://multi.source.callback/callback\",\n"
                + "   \"caption\":\"caption\",\n"
                + "   \"captionLocaleId\":\"1033\"\n"
                + "}";
                
        final String convert =
                "{\"fileUrl\":\"http:\\/\\/localhost:38081\\/office.jpg\",\"providedName\":\"office.jpg\",\"fileName\":\"429_1_office.jpg\",\"domain\":\"Lodging\",\"callback\":\"http:\\/\\/multi.source.callback\\/callback\",\"active\":\"true\",\"stagingKey\":{\"externalId\":\"222\",\"providerId\":\"300\",\"sourceId\":\"99\"},\"domainId\":\"429\",\"domainFields\":{\"subcategoryId\":\"801\",\"caption\":\"caption\",\"captionLocaleId\":\"1033\"},\"domainProvider\":\"EPC Internal User\",\"userId\":\"Multisource\",\"clientId\":\"Multisource\"}";
                
        final ImageMessage imageMessageOld = ImageMessage.parseJsonMessage(message);
        final Map messageMap = JSONUtil.buildMapFromJson(message);
        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");
        
        final String mediaCommonMessage = JSONUtil.convertToCommonMessage(imageMessageOld, messageMap, properties);
        assertEquals(convert, mediaCommonMessage);
    }
    
    @Test
    public void testFileAlreadyConatinsExpediaId() throws Exception {
        final String message = "{ \n"
                + "   \"mediaProviderId\":\"1\",\n"
                + "  \"domain\": \"Lodging\",\n"
                + "  \"domainId\": \"4608680\",\n"
                + "     \"fileUrl\":\"http://localhost:38081/4608680_300_office.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n"
                + "   \"stagingKey\":{ \n"
                + "      \"externalId\":\"222\",\n"
                + "      \"providerId\":\"300\",\n"
                + "      \"sourceId\":\"99\"\n"
                + "   },\n"
                + "   \"expediaId\":4608680,\n"
                + "   \"categoryId\":\"801\",\n"
                + "   \"callback\":\"http://multi.source.callback/callback\"\n"
                + "}";
                
        final String convert =
                "{\"fileUrl\":\"http:\\/\\/localhost:38081\\/4608680_300_office.jpg\",\"providedName\":\"4608680_300_office.jpg\",\"fileName\":\"4608680_300_office.jpg\",\"domain\":\"Lodging\",\"callback\":\"http:\\/\\/multi.source.callback\\/callback\",\"active\":\"true\",\"stagingKey\":{\"externalId\":\"222\",\"providerId\":\"300\",\"sourceId\":\"99\"},\"domainId\":\"4608680\",\"domainFields\":{\"subcategoryId\":\"801\"},\"domainProvider\":\"EPC Internal User\",\"userId\":\"Multisource\",\"clientId\":\"Multisource\"}";
        final ImageMessage imageMessageOld = ImageMessage.parseJsonMessage(message);
        final Map messageMap = JSONUtil.buildMapFromJson(message);
        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");
        
        final String mediaCommonMessage = JSONUtil.convertToCommonMessage(imageMessageOld, messageMap, properties);
        assertEquals(convert, mediaCommonMessage);
    }
    
    
    @Test
    public void testMoveCaptionFromRootToDomainfields() throws Exception {
        final String message = "{ \n"
                + "   \"mediaProviderId\":\"1\",\n"
                + "  \"domain\": \"Lodging\",\n"
                + "  \"domainId\": \"4608680\",\n"
                + "     \"fileUrl\":\"http://localhost:38081/4608680_300_office.jpg\",\n"
                + "   \"imageType\":\"Lodging\",\n"
                + "   \"stagingKey\":{ \n"
                + "      \"externalId\":\"222\",\n"
                + "      \"providerId\":\"300\",\n"
                + "      \"sourceId\":\"99\"\n"
                + "   },\n"
                + "   \"expediaId\":4608680,\n"
                + "   \"categoryId\":\"801\",\n"
                + "   \"callback\":\"http://multi.source.callback/callback\",\n"
                + "   \"caption\":\"caption to move\",\n"
                + "   \"captionLocaleId\":\"1033\"\n"
                + "}";
                
        final String convert =
                "{\"fileUrl\":\"http:\\/\\/localhost:38081\\/4608680_300_office.jpg\",\"providedName\":\"4608680_300_office.jpg\",\"fileName\":\"4608680_300_office.jpg\",\"domain\":\"Lodging\",\"callback\":\"http:\\/\\/multi.source.callback\\/callback\",\"active\":\"true\",\"stagingKey\":{\"externalId\":\"222\",\"providerId\":\"300\",\"sourceId\":\"99\"},\"domainId\":\"4608680\",\"domainFields\":{\"subcategoryId\":\"801\",\"caption\":\"caption to move\",\"captionLocaleId\":\"1033\"},\"domainProvider\":\"EPC Internal User\",\"userId\":\"Multisource\",\"clientId\":\"Multisource\"}";
        final ImageMessage imageMessageOld = ImageMessage.parseJsonMessage(message);
        final Map messageMap = JSONUtil.buildMapFromJson(message);
        final Properties properties = new Properties();
        properties.put("1", "EPC Internal User");
        
        final String mediaCommonMessage = JSONUtil.convertToCommonMessage(imageMessageOld, messageMap, properties);
        final ImageMessage convertCommonMessage = ImageMessage.parseJsonMessage(mediaCommonMessage);
        assertEquals(convert, mediaCommonMessage);
        assertEquals("caption to move", convertCommonMessage.getOuterDomainData().getDomainFieldValue("caption"));
    }

}
