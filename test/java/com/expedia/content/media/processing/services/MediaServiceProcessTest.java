package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.ImageType;
import com.expedia.content.media.processing.pipeline.domain.ImageTypeComponentPicker;
import com.expedia.content.media.processing.pipeline.reporting.Activity;
import com.expedia.content.media.processing.pipeline.reporting.LodgingLogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.services.validator.ExpediaIdValidator;
import com.expedia.content.media.processing.services.validator.MediaMessageValidator;
import com.expedia.content.media.processing.services.validator.NumericValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    @Mock
    private RabbitTemplate rabbitTemplateMock;
    @Mock
    private Reporting reporting;

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
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
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
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
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
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        ImageMessage imageMessageMock = mock(ImageMessage.class);
        when(imageMessageMock.getImageType()).thenReturn(ImageType.LODGING);
        when(imageMessageMock.getFileUrl()).thenReturn(new URL("http://media.com/img1.jpg"));
        mediaServiceProcess.publishMsg(imageMessageMock);
        verify(rabbitTemplateMock, times(1)).convertAndSend(anyString());
        verify(lodgingProcessMock, times(1))
                .log(any(URL.class), anyString(), eq(Activity.MEDIA_MESSAGE_RECEIVED), any(Date.class), eq(reporting), eq(ImageType.LODGING));
    }
}
