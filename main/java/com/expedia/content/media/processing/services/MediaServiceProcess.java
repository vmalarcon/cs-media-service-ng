package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.domain.ImageTypeComponentPicker;
import com.expedia.content.media.processing.pipeline.retry.RetryableMethod;
import com.expedia.content.media.processing.pipleline.reporting.Activity;
import com.expedia.content.media.processing.pipleline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipleline.reporting.Reporting;
import com.expedia.content.media.processing.services.validator.MediaMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * MediaServiceProcess is called by main class
 */
@Component
public class MediaServiceProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceProcess.class);
    private final List<MediaMessageValidator> validators;
    private final RabbitTemplate rabbitTemplate;
    private final ImageTypeComponentPicker<LogActivityProcess> logActivityPicker;
    private final Reporting reporting;

    /**
     * @param validators     injected from spring configuration file
     * @param rabbitTemplate jms queue template
     */
    public MediaServiceProcess(List<MediaMessageValidator> validators, RabbitTemplate rabbitTemplate,
            @Qualifier("logActivityPicker") final ImageTypeComponentPicker<LogActivityProcess> logActivityPicker, final Reporting reporting) {
        this.validators = validators;
        this.rabbitTemplate = rabbitTemplate;
        this.logActivityPicker = logActivityPicker;
        this.reporting = reporting;
    }

    /**
     * publish message to jms queue
     *
     * @param message is the received json format message from main application
     */
    @RetryableMethod
    public void publishMsg(String message) {
        try {
            rabbitTemplate.convertAndSend(message);
            LOGGER.debug("send message to queue done: [{}]", message);
            ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
            logActivity(imageMessage, Activity.MEDIA_MESSAGE_RECEIVED);
        } catch (Exception e) {
            LOGGER.error("disPatchMessage fail", e);
            throw new RuntimeException("Failed to send messge to queue", e);
        }
    }

    /**
     * validate whether the message is valid
     * numeric: mediaProviderId, expediaId,categoryId,
     * required: expediaId, fileUrl
     * URL pattern: fileURL, callback
     *
     * @param imageMessage is the received json format message from main application
     * @return ValidationStatus contain two validation status, true-successful,
     * false- validation fail , in false case, a validation message is set in ValidationStatus
     * @throws Exception in case like jms connection is down
     */
    public ValidationStatus validateImage(String imageMessage) throws Exception {
        ValidationStatus validationStatus = new ValidationStatus();
        LOGGER.debug("Validating: {}", imageMessage.toString());
        ImageMessage imageMessageObj = null;
        try {
            imageMessageObj = ImageMessage.parseJsonMessage(imageMessage);
        } catch (MalformedURLException malformException) {
            LOGGER.error("parseJson message error", malformException);
            validationStatus.setValid(false);
            validationStatus.setMessage(malformException.getMessage());
            return validationStatus;
        }
        for (MediaMessageValidator validator : validators) {
            validationStatus = validator.validate(imageMessageObj);
            if (!validationStatus.isValid()) {
                return validationStatus;
            }
        }
        return validationStatus;
    }

    /**
     * Logs a completed activity and its time.
     *
     * @param imageMessage The imageMessage of the file being processed.
     * @param activity     The activity to log.
     */
    private void logActivity(ImageMessage imageMessage, Activity activity) throws URISyntaxException {
        URL imageUrl = imageMessage.getImageUrl();
        String fileName = imageMessage.getExpediaId() + "_" + parseFileName(imageUrl.toString());
        LogActivityProcess logActivityProcess = logActivityPicker.getImageTypeComponent(imageMessage.getImageType());
        logActivityProcess.log(imageUrl, fileName, activity, new Date(), reporting, imageMessage.getImageType());
    }

    /**
     * parse the file name from http://images.com/dir1/img1.jpg
     *
     * @param url
     * @return image file name
     */
    private String parseFileName(String url) {
        int location = url.lastIndexOf("/");
        if (location > 0) {
            return url.substring(location + 1);
        }
        return url;
    }

}
