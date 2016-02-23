package com.expedia.content.media.processing.services;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import com.expedia.content.media.processing.services.dao.ProcessLogDao;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.util.ActivityMapping;
import com.expedia.content.media.processing.services.validator.MediaNamesValidator;
import com.expedia.content.media.processing.services.validator.RequestMessageValidator;

@RunWith(MockitoJUnitRunner.class)
public class StatusControllerTest {

    private static List<ActivityMapping> whitelist = new ArrayList<>();

    @Mock
    private ProcessLogDao lcmProcessLogDao;

    @BeforeClass
    public static void setUpClass() {
        ActivityMapping activityMapping1 = new ActivityMapping();
        activityMapping1.setActivityType("Reception");
        activityMapping1.setMediaType(".*");
        activityMapping1.setStatusMessage("RECEIVED");

        ActivityMapping activityMapping2 = new ActivityMapping();
        activityMapping2.setActivityType("DerivativeCreation");
        activityMapping2.setMediaType("VirtualTour|Lodging");
        activityMapping2.setStatusMessage("DERIVATIVES_CREATED");

        ActivityMapping activityMapping3 = new ActivityMapping();
        activityMapping3.setActivityType("Reject");
        activityMapping3.setMediaType(".*");
        activityMapping3.setStatusMessage("REJECTED");

        ActivityMapping activityMapping4 = new ActivityMapping();
        activityMapping4.setActivityType("Publish");
        activityMapping4.setMediaType(".*");
        activityMapping4.setStatusMessage("PUBLISHED");

        ActivityMapping activityMapping5 = new ActivityMapping();
        activityMapping5.setActivityType("DcpPickup");
        activityMapping5.setMediaType("Cars");
        activityMapping5.setStatusMessage("RECEIVED");

        ActivityMapping activityMapping6 = new ActivityMapping();
        activityMapping6.setActivityType("DerivativeCreation");
        activityMapping6.setMediaType("Cars");
        activityMapping6.setStatusMessage("PUBLISHED");

        whitelist.add(activityMapping1);
        whitelist.add(activityMapping2);
        whitelist.add(activityMapping3);
        whitelist.add(activityMapping4);
        whitelist.add(activityMapping5);
        whitelist.add(activityMapping6);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusPublished() throws Exception {
        List<RequestMessageValidator> validators = new ArrayList<>();
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "Publish", "Lodging");
        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        StatusController statusController = new StatusController();
        setFieldValue(statusController, "mediaStatusValidatorList", validators);
        setFieldValue(statusController, "activityWhiteList", whitelist);
        setFieldValue(statusController, "processLogDao", lcmProcessLogDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"PUBLISHED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}",
                response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusBeingProcessed() throws Exception {
        List<RequestMessageValidator> validators = new ArrayList<>();
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatusRece = new MediaProcessLog("2014-07-29 10:06:12.6890000 -07:00", "1037678_109010ice.jpg", "Reception", "Lodging");
        MediaProcessLog mediaLogStatus =
                new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "DerivativeCreation", "Lodging");
        mediaLogStatuses.add(mediaLogStatusRece);

        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        StatusController statusController = new StatusController();
        setFieldValue(statusController, "mediaStatusValidatorList", validators);
        setFieldValue(statusController, "activityWhiteList", whitelist);
        setFieldValue(statusController, "processLogDao", lcmProcessLogDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"DERIVATIVES_CREATED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}",
                response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusCarReceived() throws Exception {
        List<RequestMessageValidator> validators = new ArrayList<>();
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();

        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "DcpPickup", "Cars");
        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        StatusController statusController = new StatusController();
        setFieldValue(statusController, "mediaStatusValidatorList", validators);
        setFieldValue(statusController, "activityWhiteList", whitelist);
        setFieldValue(statusController, "processLogDao", lcmProcessLogDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"RECEIVED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}",
                response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusCarDeriCreated() throws Exception {
        List<RequestMessageValidator> validators = new ArrayList<>();
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();

        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "DerivativeCreation", "Cars");
        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        StatusController statusController = new StatusController();
        setFieldValue(statusController, "mediaStatusValidatorList", validators);
        setFieldValue(statusController, "activityWhiteList", whitelist);
        setFieldValue(statusController, "processLogDao", lcmProcessLogDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"PUBLISHED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}",
                response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusRejected() throws Exception {
        List<RequestMessageValidator> validators = new ArrayList<>();
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatusRece = new MediaProcessLog("2014-07-29 10:06:12.6890000 -07:00", "1037678_109010ice.jpg", "Reception", "Lodging");
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "Reject", "Lodging");
        mediaLogStatuses.add(mediaLogStatusRece);

        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        StatusController statusController = new StatusController();
        setFieldValue(statusController, "mediaStatusValidatorList", validators);
        setFieldValue(statusController, "activityWhiteList", whitelist);
        setFieldValue(statusController, "processLogDao", lcmProcessLogDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"REJECTED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}",
                response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusUnrecognized() throws Exception {
        List<RequestMessageValidator> validators = new ArrayList<>();
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus =
                new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "mpp_collector/test", "Lodging");

        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        StatusController statusController = new StatusController();
        setFieldValue(statusController, "mediaStatusValidatorList", validators);
        setFieldValue(statusController, "activityWhiteList", whitelist);
        setFieldValue(statusController, "processLogDao", lcmProcessLogDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"NOT_FOUND\"}]}", response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaSuccessFilterArchive() throws Exception {
        List<RequestMessageValidator> validators = new ArrayList<>();
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<MediaProcessLog>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "Publish", "Lodging");
        MediaProcessLog mediaLogStatusArchive = new MediaProcessLog("2014-07-29 10:09:12.6890000 -07:00", "1037678_109010ice.jpg", "Archive", "Lodging");

        mediaLogStatuses.add(mediaLogStatus);
        mediaLogStatuses.add(mediaLogStatusArchive);
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        StatusController statusController = new StatusController();
        setFieldValue(statusController, "mediaStatusValidatorList", validators);
        setFieldValue(statusController, "activityWhiteList", whitelist);
        setFieldValue(statusController, "processLogDao", lcmProcessLogDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"PUBLISHED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}",
                response.getBody());
    }

    @SuppressWarnings({"rawtypes"})
    @Test
    public void testWrongPropertyName() throws Exception {
        List<RequestMessageValidator> validators = new ArrayList<>();
        MediaNamesValidator mediaNameValidator = new MediaNamesValidator();
        setFieldValue(mediaNameValidator, "maximumRequestCount", 10);
        validators.add(mediaNameValidator);

        StatusController statusController = new StatusController();
        setFieldValue(statusController, "mediaStatusValidatorList", validators);
        setFieldValue(statusController, "activityWhiteList", whitelist);
        setFieldValue(statusController, "processLogDao", lcmProcessLogDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNamesaa\":[\"1037678_109010ice.jpg\",\"1055797_1742165ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().startsWith(
                "{\"error\":\"Bad Request\",\"message\":\"message does not contain property 'messageNames'.\",\"path\":\"/media/v1/lateststatus\",\"status\":400,\"timestamp\""));
    }

    @SuppressWarnings({"rawtypes"})
    @Test
    public void testTooManyNames() throws Exception {
        List<RequestMessageValidator> validators = new ArrayList<>();
        MediaNamesValidator mediaNameValidator = new MediaNamesValidator();
        setFieldValue(mediaNameValidator, "maximumRequestCount", 1);
        validators.add(mediaNameValidator);

        StatusController statusController = new StatusController();
        setFieldValue(statusController, "mediaStatusValidatorList", validators);
        setFieldValue(statusController, "activityWhiteList", whitelist);
        setFieldValue(statusController, "processLogDao", lcmProcessLogDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\",\"1055797_1742165ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().startsWith(
                "{\"error\":\"Bad Request\",\"message\":\"messageNames count exceed the maximum 1\",\"path\":\"/media/v1/lateststatus\",\"status\":400,\"timestamp\""));
    }

    @SuppressWarnings({"rawtypes"})
    @Test
    public void testWrongFormatMediaStatusMessage() throws Exception {
        List<RequestMessageValidator> validators = new ArrayList<>();
        MediaNamesValidator mediaNameValidator = new MediaNamesValidator();
        setFieldValue(mediaNameValidator, "maximumRequestCount", 10);
        validators.add(mediaNameValidator);

        StatusController statusController = new StatusController();
        setFieldValue(statusController, "mediaStatusValidatorList", validators);
        setFieldValue(statusController, "activityWhiteList", whitelist);
        setFieldValue(statusController, "processLogDao", lcmProcessLogDao);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":\"1037678_109010ice.jpg\",\"1055797_1742165ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().toString().startsWith(
                "{\"error\":\"Bad Request\",\"message\":\"Error parsing/converting Json message: {\\\"mediaNames\\\":\\\"1037678_109010ice.jpg\\\",\\\"1055797_1742165ice.jpg\\\"]}\",\"path\":\"/media/v1/lateststatus\",\"status\":400,\"timestamp\""));
    }

}
