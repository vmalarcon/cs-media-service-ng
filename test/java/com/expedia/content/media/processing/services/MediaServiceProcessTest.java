package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageType;
import com.expedia.content.media.processing.domain.ImageTypeComponentPicker;
import com.expedia.content.media.processing.pipleline.reporting.LodgingLogActivityProcess;
import com.expedia.content.media.processing.pipleline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipleline.reporting.Reporting;
import com.expedia.content.media.processing.services.validator.ExpediaIdValidator;
import com.expedia.content.media.processing.services.validator.MediaMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by seli on 2015-07-14.
 */
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
        validators.add(expediaIdValidator);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        LodgingLogActivityProcess lodgingLogActivityProcess = mock(LodgingLogActivityProcess.class);
        when(mockLogActivityPicker.getImageTypeComponent(ImageType.LODGING)).thenReturn(lodgingLogActivityProcess);
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock, mockLogActivityPicker, reporting);
        ValidationStatus validationStatus = mediaServiceProcess.validateImage(jsonMessage);
        org.junit.Assert.assertTrue(validationStatus.isValid());
    }

    @Test
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
        ValidationStatus validationStatus = mediaServiceProcess.validateImage(jsonMessage);
        org.junit.Assert.assertFalse(validationStatus.isValid());
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
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doNothing().when(rabbit).convertAndSend(jsonMessage);
        ImageTypeComponentPicker<LogActivityProcess> mockLogActivityPicker = mock(ImageTypeComponentPicker.class);
        LodgingLogActivityProcess lodgingLogActivityProcess = mock(LodgingLogActivityProcess.class);
        when(mockLogActivityPicker.getImageTypeComponent(ImageType.LODGING)).thenReturn(lodgingLogActivityProcess);

        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbit, mockLogActivityPicker, reporting);
        mediaServiceProcess.publishMsg(jsonMessage);
    }

}
