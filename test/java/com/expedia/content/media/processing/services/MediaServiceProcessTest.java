package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.domain.ImageType;
import com.expedia.content.media.processing.domain.ImageTypeComponentPicker;
import com.expedia.content.media.processing.pipleline.reporting.LodgingLogActivityProcess;
import com.expedia.content.media.processing.pipleline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipleline.reporting.Reporting;
import com.expedia.content.media.processing.services.validator.ExpediaIdValidator;
import com.expedia.content.media.processing.services.validator.MediaMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MediaServiceProcessTest {

    @Mock
    private RabbitTemplate rabbitTemplateMock;
    @Mock
    private Reporting reporting;

    @Test
    public void testvalidateImageSuccess() throws Exception {

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
                "   \"callBack\":\"http://multi.source.callback/callback\",\n" +
                "   \"caption\":\"caption\"\n" +
                "}";
        List<MediaMessageValidator> validators = new ArrayList<>();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        expediaIdValidator.setFieldName("expediaId");
        validators.add(expediaIdValidator);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        LodgingLogActivityProcess lodgingLogActivityProcess = mock(LodgingLogActivityProcess.class);
        when(mockLogActivityPicker.getImageTypeComponent(ImageType.LODGING)).thenReturn(lodgingLogActivityProcess);
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        ImageMessage imageMessage = mediaServiceProcess.parseJsonMessage(jsonMessage);
        ValidationStatus validationStatus = mediaServiceProcess.validateImage(imageMessage);
        org.junit.Assert.assertTrue(validationStatus.isValid());
    }

    @Test(expected = MalformedURLException.class)
    public void testvalidateImageFail() throws Exception {

        String jsonMessage = "{  \n" +
                "   \"mediaProviderId\":\"1001\",\n" +
                "   \"fileUrl\":\"httpp://images.com/dir1/img1.jpg\",\n" +
                "   \"imageType\":\"Lodging\",\n" +
                "   \"stagingKeyMap\":{  \n" +
                "      \"externalId\":\"222\",\n" +
                "      \"providerId\":\"300\",\n" +
                "      \"sourceId\":\"99\"\n" +
                "   },\n" +
                "   \"expediaId\":2001002,\n" +
                "   \"categoryId\":\"801\",\n" +
                "   \"callBack\":\"http://multi.source.callback/callback\",\n" +
                "   \"caption\":\"caption\"\n" +
                "}";
        List<MediaMessageValidator> validators = new ArrayList<>();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        validators.add(expediaIdValidator);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        LodgingLogActivityProcess lodgingLogActivityProcess = mock(LodgingLogActivityProcess.class);
        when(mockLogActivityPicker.getImageTypeComponent(ImageType.LODGING)).thenReturn(lodgingLogActivityProcess);
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        ImageMessage imageMessage = mediaServiceProcess.parseJsonMessage(jsonMessage);
    }

    @Test
    public void testPublishMessage() throws Exception {

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
                "   \"callBack\":\"http://multi.source.callback/callback\",\n" +
                "   \"caption\":\"caption\"\n" +
                "}";
        List<MediaMessageValidator> validators = new ArrayList<>();
        ExpediaIdValidator expediaIdValidator = new ExpediaIdValidator();
        validators.add(expediaIdValidator);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        LodgingLogActivityProcess lodgingLogActivityProcess = mock(LodgingLogActivityProcess.class);
        when(mockLogActivityPicker.getImageTypeComponent(ImageType.LODGING)).thenReturn(lodgingLogActivityProcess);
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        mediaServiceProcess.publishMsg(jsonMessage);
        verify(rabbitTemplateMock, times(1)).convertAndSend(jsonMessage);

    }

}
