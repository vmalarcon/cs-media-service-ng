package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.retry.RetryableMethod;
import com.expedia.content.media.processing.services.validator.MediaMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.util.List;

/**
 * MediaServiceProcess is called by main class
 */
@Component
public class MediaServiceProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceProcess.class);
    private final List<MediaMessageValidator> validators;
    private final RabbitTemplate rabbitTemplate;

    /**
     * @param validators     injected from spring configuration file
     * @param rabbitTemplate jms queue template
     */
    public MediaServiceProcess(List<MediaMessageValidator> validators, RabbitTemplate rabbitTemplate) {
        this.validators = validators;
        this.rabbitTemplate = rabbitTemplate;
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
        } catch (Exception e) {
            LOGGER.error("disPatchMessage fail", e);
            throw new RuntimeException("Failed to send messge to queue", e);
        }
    }



    /**
     *validate whether the message is valid
     * numeric: mediaProviderId, expediaId,categoryId,
     * required: expediaId, fileUrl
     * URL pattern: fileURL, callback
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
            validationStatus.setStatus(false);
            validationStatus.setMessage(malformException.getMessage());
            return validationStatus;
        }
        for (MediaMessageValidator validator : validators) {
            validationStatus = validator.validate(imageMessageObj);
            if (!validationStatus.isStatus()) {
                return validationStatus;
            }
        }
        return validationStatus;
    }

}
