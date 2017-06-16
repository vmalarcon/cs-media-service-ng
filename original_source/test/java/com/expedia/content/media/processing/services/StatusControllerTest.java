package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.util.ActivityMapping;
import com.expedia.content.media.processing.services.validator.MediaNamesValidator;
import com.expedia.content.media.processing.services.validator.RequestMessageValidator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StatusControllerTest {

    @Mock
    private MediaDao mockMediaDao;
    @Mock
    private Poker poker;

    private StatusController statusController;

    @Before
    public void initialize() throws NoSuchFieldException, IllegalAccessException {
        List<RequestMessageValidator> validators = new ArrayList<>();
        statusController = new StatusController(validators, mockMediaDao, poker);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusPublished() throws Exception {
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "PUBLISHED", "Lodging");
        mediaLogStatuses.add(mediaLogStatus);
        when(mockMediaDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String filename = "1037678_109010ice.jpg";
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add(filename);

        String message = "{\"mediaNames\":[\"" + filename + "\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"PUBLISHED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}",
                response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatuses() throws Exception {
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus1 = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "RECEIVED", "Lodging");
        MediaProcessLog mediaLogStatus2 = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1234567_891011ice.jpg", "RECEIVED", "Lodging");
        MediaProcessLog mediaLogStatus3 = new MediaProcessLog("2014-08-01 10:08:12.6890000 -07:00", "1234567_891011ice.jpg", "DUPLICATE", "Lodging");
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus2);
        mediaLogStatuses.add(mediaLogStatus3);

        when(mockMediaDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\",\"1234567_891011ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"RECEIVED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}"));
        assertTrue(response.getBody().toString().contains("{\"mediaName\":\"1234567_891011ice.jpg\",\"status\":\"DUPLICATE\",\"time\":\"2014-08-01 10:08:12.6890000 -07:00\"}"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusBeingProcessed() throws Exception {
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus1 = new MediaProcessLog("2014-07-29 10:06:12.6890000 -07:00", "1037678_109010ice.jpg", "RECEIVED", "Lodging");
        MediaProcessLog mediaLogStatus2 = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "DERIVATIVES_CREATED", "Lodging");
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus2);
        when(mockMediaDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"DERIVATIVES_CREATED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}",
                response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusCarReceived() throws Exception {
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "RECEIVED", "Cars");
        mediaLogStatuses.add(mediaLogStatus);
        when(mockMediaDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"RECEIVED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}",
                response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusCarDerivCreated() throws Exception {
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "PUBLISHED", "Cars");
        mediaLogStatuses.add(mediaLogStatus);
        when(mockMediaDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"PUBLISHED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}",
                response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusRejected() throws Exception {
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus1 = new MediaProcessLog("2014-07-29 10:06:12.6890000 -07:00", "1037678_109010ice.jpg", "RECEIVED", "Lodging");
        MediaProcessLog mediaLogStatus2 = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "REJECTED", "Lodging");
        mediaLogStatuses.add(mediaLogStatus1);
        mediaLogStatuses.add(mediaLogStatus2);
        when(mockMediaDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"REJECTED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}",
                response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaStatusUnrecognized() throws Exception {
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus =
                new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "mpp_collector/test", "Lodging");
        mediaLogStatuses.add(mediaLogStatus);
        when(mockMediaDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"NOT_FOUND\"}]}", response.getBody());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetMediaSuccessFilterArchive() throws Exception {
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "PUBLISHED", "Lodging");
        MediaProcessLog mediaLogStatusArchive = new MediaProcessLog("2014-07-29 10:09:12.6890000 -07:00", "1037678_109010ice.jpg", "Archive", "Lodging");
        mediaLogStatuses.add(mediaLogStatus);
        mediaLogStatuses.add(mediaLogStatusArchive);
        when(mockMediaDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
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
        setFieldValue(statusController, "mediaStatusValidatorList", validators);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNamesAndOtherThings\":[\"1037678_109010ice.jpg\",\"1055797_1742165ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
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
        setFieldValue(statusController, "mediaStatusValidatorList", validators);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\",\"1055797_1742165ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
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
        setFieldValue(statusController, "mediaStatusValidatorList", validators);

        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);

        String message = "{\"mediaNames\":\"1037678_109010ice.jpg\",\"1055797_1742165ice.jpg\"]}";
        ResponseEntity response = statusController.getMediaLatestStatus(message, mockHeader);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().startsWith(
                "{\"error\":\"Bad Request\",\"message\":\"Error parsing/converting Json message: {\\\"mediaNames\\\":\\\"1037678_109010ice.jpg\\\",\\\"1055797_1742165ice.jpg\\\"]}\",\"path\":\"/media/v1/lateststatus\",\"status\":400,\"timestamp\""));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test(expected = RuntimeException.class)
    public void pokeTest() throws Exception {
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "PUBLISHED", "Lodging");
        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("1037678_109010ice.jpg");
        RuntimeException exception = new RuntimeException("this is a runtime exception");
        when(mockMediaDao.findMediaStatus(anyList())).thenThrow(exception);
        setFieldValue(statusController, "hipChatRoom", "EWE CS: Phoenix Notifications");
        String requestId = "test-request-id";
        MultiValueMap<String, String> mockHeader = new HttpHeaders();
        mockHeader.add("request-id", requestId);
        String message = "{\"mediaNames\":[\"1037678_109010ice.jpg\"]}";
        statusController.getMediaLatestStatus(message, mockHeader);
        verify(poker).poke(eq("Media Services failed to process a getMediaLatestStatus request - RequestId: " + requestId), eq("EWE CS: Phoenix Notifications"),
                eq(message), eq(exception));

    }

}
