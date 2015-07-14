package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.domain.ImageType;
import com.expedia.content.media.processing.domain.StagingKeyMap;
import com.expedia.content.media.processing.services.validator.CategoryIdValidator;
import com.expedia.content.media.processing.services.validator.ExpediaIdValidator;
import com.expedia.content.media.processing.services.validator.MediaMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import junit.framework.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by seli on 2015-07-14.
 */
public class MediaServiceProcessTest {

    @Mock
    private RabbitTemplate rabbitTemplateMock;

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
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock);
        ValidationStatus validationStatus = mediaServiceProcess.validateImage(jsonMessage);
        org.junit.Assert.assertTrue(validationStatus.isStatus());
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
        MediaServiceProcess mediaServiceProcess = new MediaServiceProcess(validators, rabbitTemplateMock);
        ValidationStatus validationStatus = mediaServiceProcess.validateImage(jsonMessage);
        org.junit.Assert.assertFalse(validationStatus.isStatus());
    }

}
