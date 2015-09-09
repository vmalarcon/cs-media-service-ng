package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.domain.ImageType;
import com.expedia.content.media.processing.domain.ImageTypeComponentPicker;
import com.expedia.content.media.processing.pipleline.reporting.Activity;
import com.expedia.content.media.processing.pipleline.reporting.LodgingLogActivityProcess;
import com.expedia.content.media.processing.pipleline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipleline.reporting.Reporting;
import com.expedia.content.media.processing.pipleline.reporting.sql.MediaProcessLog;
import com.expedia.content.media.processing.services.validator.ExpediaIdValidator;
import com.expedia.content.media.processing.services.validator.MediaMessageValidator;
import com.expedia.content.media.processing.services.validator.NumericValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MediaServiceProcessTest {
    private static Map<String, String> maps = new HashMap<>();
    private static List<String> filterActivityList = new ArrayList<>();
    @Mock
    private RabbitTemplate rabbitTemplateMock;
    @Mock
    private Reporting reporting;

    @BeforeClass
    public static void setUpClass() {
        maps.put("Publish", "Media is published");
        maps.put("Reception", "Media File Received");
        maps.put("MediaImport", "Media is being processed");
        maps.put("DcpPickup", "Media is being processed");
        maps.put("PreProcess", "Media is being processed");
        maps.put("DerivativeCalculation", "Media is being processed");
        maps.put("DerivativeCreation", "Media is being processed");
        maps.put("PostProcess", "Media is being processed");
        maps.put("Derivatives", "Media is being processed");
        maps.put("Reject", "Media is corrupted");
        filterActivityList.add("Archive");
        filterActivityList.add("MediaUpload");
    }

    @Test
    public void testValidateImageSuccess() throws Exception {

        String jsonMessage = "{  \n" +
                "   \"mediaProviderId\":\"1001\",\n" +
                "   \"fileUrl\":\"http://images.com/dir1/img1.jpg\",\n" +
                "   \"imageType\":\"Lodging\",\n" +
                "   \"stagingKeyMap\":{  \n" +
                "      \"externalId\":\"222\",\n" +
                "      \"providerId\":\"300\",\n" +
                "      \"sourceId\":\"99\"\n" +
                "   },\n" +
                "   \"expediaId\":2001002,\n" +
                "   \"categoryId\":\"801\",\n" +
                "   \"callback\":\"http://multi.source.callback/callback\",\n" +
                "   \"caption\":\"caption\"\n" +
                "}";
        List<MediaMessageValidator> validators = new ArrayList<>();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        validators.add(expediaIdValidator);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting, maps);
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMessage);
        ValidationStatus validationStatus = mediaServiceProcess.validateImage(imageMessage);
        assertTrue(validationStatus.isValid());
    }

    @Test
    public void testValidateImageFail() throws Exception {

        String jsonMessage = "{  \n" +
                "   \"mediaProviderId\":\"1001\",\n" +
                "   \"fileUrl\":\"http://images.com/dir1/img1.jpg\",\n" +
                "   \"imageType\":\"Lodging\",\n" +
                "   \"stagingKeyMap\":{  \n" +
                "      \"externalId\":\"222\",\n" +
                "      \"providerId\":\"300\",\n" +
                "      \"sourceId\":\"99\"\n" +
                "   },\n" +
                "   \"expediaId\":2001002,\n" +
                "   \"categoryId\":\"NOT_NUMBER\",\n" +
                "   \"callback\":\"http://multi.source.callback/callback\",\n" +
                "   \"caption\":\"caption\"\n" +
                "}";
        List<MediaMessageValidator> validators = new ArrayList<>();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        NumericValidator numericValidator = new NumericValidator();
        numericValidator.setFieldName("categoryId");

        validators.add(expediaIdValidator);
        validators.add(numericValidator);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting, maps);
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMessage);
        ValidationStatus validationStatus = mediaServiceProcess.validateImage(imageMessage);
        assertFalse(validationStatus.isValid());
    }

    @Test
    public void testPublishMessage() throws Exception {
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        LodgingLogActivityProcess lodgingProcessMock = mock(LodgingLogActivityProcess.class);
        when(mockLogActivityPicker.getImageTypeComponent(ImageType.LODGING)).thenReturn(lodgingProcessMock);
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting, maps);
        mediaServiceProcess.setFilterActivityList(filterActivityList);
        ImageMessage imageMessageMock = mock(ImageMessage.class);
        when(imageMessageMock.getImageType()).thenReturn(ImageType.LODGING);
        when(imageMessageMock.getFileUrl()).thenReturn(new URL("http://media.com/img1.jpg"));
        mediaServiceProcess.publishMsg(imageMessageMock);
        verify(rabbitTemplateMock, times(1)).convertAndSend(anyString());
        verify(lodgingProcessMock, times(1))
                .log(any(URL.class), anyString(), eq(Activity.MEDIA_MESSAGE_RECEIVED), any(Date.class), eq(reporting), eq(ImageType.LODGING));
    }

    @Test
    public void testGetMediaStatusPublished() throws Exception {
        String jsonMessage =
                "{\"mediaStatuses\":"
                        + "[{\"statuses\":[{\"time\":\"2014-07-29 10:08:12.6890000 -07:00\",\"status\":\"Media is published\"}],\"mediaName\":\"1037678_109010ice.jpg\"}]}";
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "LCM/Publish");
        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList();
        fileNameList.add("1037678_109010ice.jpg");
        when(reporting.findMediaStatus(anyString())).thenReturn(mediaLogStatuses);

        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting, maps);
        mediaServiceProcess.setFilterActivityList(filterActivityList);
        String response = mediaServiceProcess.getMediaStatusList(fileNameList);
        assertTrue(response.equals(jsonMessage));
    }

    @Test
    public void testGetMediaStatusBeingProcessed() throws Exception {
        String jsonMessage =
                "{\"mediaStatuses\":"
                        + "[{\"statuses\":[{\"time\":\"2014-07-29 10:08:12.6890000 -07:00\",\"status\":\"Media is being processed\"}],\"mediaName\":\"1037678_109010ice.jpg\"}]}";
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatusRece = new MediaProcessLog("2014-07-29 10:06:12.6890000 -07:00", "1037678_109010ice.jpg", "Repo1/Reception");
        MediaProcessLog mediaLogStatus =
                new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "mpp_collector/DerivativeCalculation");
        mediaLogStatuses.add(mediaLogStatusRece);

        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList();
        fileNameList.add("1037678_109010ice.jpg");
        when(reporting.findMediaStatus(anyString())).thenReturn(mediaLogStatuses);

        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting, maps);
        mediaServiceProcess.setFilterActivityList(filterActivityList);
        String response = mediaServiceProcess.getMediaStatusList(fileNameList);
        assertTrue(response.equals(jsonMessage));
    }

    @Test
    public void testGetMediaStatusRejected() throws Exception {
        String jsonMessage =
                "{\"mediaStatuses\":"
                        + "[{\"statuses\":[{\"time\":\"2014-07-29 10:08:12.6890000 -07:00\",\"status\":\"Media is corrupted\"}],\"mediaName\":\"1037678_109010ice.jpg\"}]}";
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatusRece = new MediaProcessLog("2014-07-29 10:06:12.6890000 -07:00", "1037678_109010ice.jpg", "Repo1/Reception");
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "mpp_collector/Reject");
        mediaLogStatuses.add(mediaLogStatusRece);

        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList();
        fileNameList.add("1037678_109010ice.jpg");
        when(reporting.findMediaStatus(anyString())).thenReturn(mediaLogStatuses);

        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting, maps);
        mediaServiceProcess.setFilterActivityList(filterActivityList);
        String response = mediaServiceProcess.getMediaStatusList(fileNameList);
        assertTrue(response.equals(jsonMessage));
    }

    @Test
    public void testGetMediaStatusUnrecognized() throws Exception {
        String jsonMessage =
                "{\"mediaStatuses\":"
                        + "[{\"statuses\":[{\"time\":\"2014-07-29 10:08:12.6890000 -07:00\",\"status\":\"Unrecognized status:test\"}],\"mediaName\":\"1037678_109010ice.jpg\"}]}";
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "mpp_collector/test");

        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList();
        fileNameList.add("1037678_109010ice.jpg");
        when(reporting.findMediaStatus(anyString())).thenReturn(mediaLogStatuses);

        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting, maps);
        mediaServiceProcess.setFilterActivityList(filterActivityList);
        String response = mediaServiceProcess.getMediaStatusList(fileNameList);
        assertTrue(response.equals(jsonMessage));
    }

    @Test
    public void testGetMediaSuccessFilterArchive() throws Exception {
        String jsonMessage =
                "{\"mediaStatuses\":[{\"statuses\":"
                        + "[{\"time\":\"2014-07-29 10:08:12.6890000 -07:00\",\"status\":\"Media is published\"}],\"mediaName\":\"1037678_109010ice.jpg\"}]}";
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<MediaProcessLog>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "Publish");
        MediaProcessLog mediaLogStatusArchive =
                new MediaProcessLog("2014-07-29 10:09:12.6890000 -07:00", "1037678_109010ice.jpg", "Archive");

        mediaLogStatuses.add(mediaLogStatus);
        mediaLogStatuses.add(mediaLogStatusArchive);
        List<String> fileNameList = new ArrayList();
        fileNameList.add("1037678_109010ice.jpg");
        when(reporting.findMediaStatus(anyString())).thenReturn(mediaLogStatuses);
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting, maps);
        mediaServiceProcess.setFilterActivityList(filterActivityList);
        String response = mediaServiceProcess.getMediaStatusList(fileNameList);
        assertTrue(response.equals(jsonMessage));
    }
}
