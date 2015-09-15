package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.domain.ImageType;
import com.expedia.content.media.processing.domain.ImageTypeComponentPicker;
import com.expedia.content.media.processing.pipleline.reporting.Activity;
import com.expedia.content.media.processing.pipleline.reporting.LodgingLogActivityProcess;
import com.expedia.content.media.processing.pipleline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipleline.reporting.Reporting;
import com.expedia.content.media.processing.services.dao.LcmProcessLogDao;
import com.expedia.content.media.processing.services.dao.MediaProcessLog;
import com.expedia.content.media.processing.services.util.ActivityMapping;
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
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MediaServiceProcessTest {
    private static List<ActivityMapping> whitelist = new ArrayList<>();
    @Mock
    private RabbitTemplate rabbitTemplateMock;
    @Mock
    private Reporting reporting;
    @Mock
    private LcmProcessLogDao lcmProcessLogDao;

    @BeforeClass
    public static void setUpClass() {

        ActivityMapping activityMapping1 = new ActivityMapping();
        activityMapping1.setActivityType("Reception");
        activityMapping1.setMediaType(".*");
        activityMapping1.setStatusMessage("RECEIVED");

        ActivityMapping activityMapping2 = new ActivityMapping();
        activityMapping2.setActivityType("DerivativeCreation");
        activityMapping2.setMediaType(".*");
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

        whitelist.add(activityMapping1);
        whitelist.add(activityMapping2);
        whitelist.add(activityMapping3);
        whitelist.add(activityMapping4);
        whitelist.add(activityMapping5);

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
        MediaServiceProcess mediaServiceProcess =
                new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
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
        MediaServiceProcess mediaServiceProcess =
                new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
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
        MediaServiceProcess mediaServiceProcess =
                new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        mediaServiceProcess.setActivityWhiteList(whitelist);
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
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"PUBLISHED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}";
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "Publish", "Lodging");
        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        MediaServiceProcess mediaServiceProcess =
                new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        mediaServiceProcess.setLcmProcessLogDao(lcmProcessLogDao);
        mediaServiceProcess.setActivityWhiteList(whitelist);
        String response = mediaServiceProcess.getMediaStatusList(fileNameList);
        assertTrue(response.equals(jsonMessage));
    }

    @Test
    public void testGetMediaStatusBeingProcessed() throws Exception {
        String jsonMessage =
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"DERIVATIVES_CREATED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}";
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatusRece =
                new MediaProcessLog("2014-07-29 10:06:12.6890000 -07:00", "1037678_109010ice.jpg", "Reception", "Lodging");
        MediaProcessLog mediaLogStatus =
                new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "DerivativeCreation", "Lodging");
        mediaLogStatuses.add(mediaLogStatusRece);

        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        MediaServiceProcess mediaServiceProcess =
                new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        mediaServiceProcess.setLcmProcessLogDao(lcmProcessLogDao);

        mediaServiceProcess.setActivityWhiteList(whitelist);
        String response = mediaServiceProcess.getMediaStatusList(fileNameList);
        assertTrue(response.equals(jsonMessage));
    }

    @Test
    public void testGetMediaStatusCarReceived() throws Exception {
        String jsonMessage =
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"RECEIVED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}";
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();

        MediaProcessLog mediaLogStatus =
                new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "DcpPickup", "Cars");
        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        MediaServiceProcess mediaServiceProcess =
                new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        mediaServiceProcess.setLcmProcessLogDao(lcmProcessLogDao);

        mediaServiceProcess.setActivityWhiteList(whitelist);
        String response = mediaServiceProcess.getMediaStatusList(fileNameList);
        assertTrue(response.equals(jsonMessage));
    }

    @Test
    public void testGetMediaStatusRejected() throws Exception {
        String jsonMessage =
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"REJECTED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}";
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatusRece =
                new MediaProcessLog("2014-07-29 10:06:12.6890000 -07:00", "1037678_109010ice.jpg", "Reception", "Lodging");
        MediaProcessLog mediaLogStatus =
                new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "Reject", "Lodging");
        mediaLogStatuses.add(mediaLogStatusRece);

        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        MediaServiceProcess mediaServiceProcess =
                new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        mediaServiceProcess.setLcmProcessLogDao(lcmProcessLogDao);

        mediaServiceProcess.setActivityWhiteList(whitelist);
        String response = mediaServiceProcess.getMediaStatusList(fileNameList);
        assertTrue(response.equals(jsonMessage));
    }

    @Test
    public void testGetMediaStatusUnrecognized() throws Exception {
        String jsonMessage =
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"NOT_FOUND\"}]}";
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<>();
        MediaProcessLog mediaLogStatus =
                new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "mpp_collector/test", "Lodging");

        mediaLogStatuses.add(mediaLogStatus);
        List<String> fileNameList = new ArrayList();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);

        MediaServiceProcess mediaServiceProcess =
                new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        mediaServiceProcess.setLcmProcessLogDao(lcmProcessLogDao);

        mediaServiceProcess.setActivityWhiteList(whitelist);
        String response = mediaServiceProcess.getMediaStatusList(fileNameList);
        assertTrue(response.equals(jsonMessage));
    }

    @Test
    public void testGetMediaSuccessFilterArchive() throws Exception {
        String jsonMessage =
                "{\"mediaStatuses\":[{\"mediaName\":\"1037678_109010ice.jpg\",\"status\":\"PUBLISHED\",\"time\":\"2014-07-29 10:08:12.6890000 -07:00\"}]}";
        List<MediaMessageValidator> validators = mock(List.class);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        List<MediaProcessLog> mediaLogStatuses = new ArrayList<MediaProcessLog>();
        MediaProcessLog mediaLogStatus = new MediaProcessLog("2014-07-29 10:08:12.6890000 -07:00", "1037678_109010ice.jpg", "Publish", "Lodging");
        MediaProcessLog mediaLogStatusArchive =
                new MediaProcessLog("2014-07-29 10:09:12.6890000 -07:00", "1037678_109010ice.jpg", "Archive", "Lodging");

        mediaLogStatuses.add(mediaLogStatus);
        mediaLogStatuses.add(mediaLogStatusArchive);
        List<String> fileNameList = new ArrayList();
        fileNameList.add("1037678_109010ice.jpg");
        when(lcmProcessLogDao.findMediaStatus(anyList())).thenReturn(mediaLogStatuses);
        MediaServiceProcess mediaServiceProcess =
                new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        mediaServiceProcess.setLcmProcessLogDao(lcmProcessLogDao);

        mediaServiceProcess.setActivityWhiteList(whitelist);
        String response = mediaServiceProcess.getMediaStatusList(fileNameList);
        assertTrue(response.equals(jsonMessage));
    }
}
