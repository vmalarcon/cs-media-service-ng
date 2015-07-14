package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.services.validator.MediaMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.util.List;

/**
 * Created by seli on 2015-07-13.
 */
@Component
public class MediaServiceProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceProcess.class);
    private final List<MediaMessageValidator> validators;

    public MediaServiceProcess(List<MediaMessageValidator> validators, RabbitTemplate rabbitTemplate) {
        this.validators = validators;
        this.rabbitTemplate = rabbitTemplate;
    }

    private final RabbitTemplate rabbitTemplate;

    /*
    *disPatch messge to jms queue
     */
    public void disPatchMsg(String message) {
        try {
            rabbitTemplate.convertAndSend(message);
            LOGGER.debug("send message to queue done: [{}]", message);
        } catch (Exception e) {
            LOGGER.error("disPatchMessage fail",e);
            throw new RuntimeException("Failed to send messge to queue", e);
        }
    }

    /*
    *validate whether the message is valid
    * numberic: mediaProviderId, expediaId,categoryId,
    * required: expediaId, fileUrl
    * URL pattern: fileURL, callback
     */
    public ValidationStatus validateImage(String imageMessage) throws Exception {
        ValidationStatus validationStatus = new ValidationStatus();
        LOGGER.debug("Validating: {}", imageMessage.toString());
        ImageMessage imageMessageObj = null;
        try {
            imageMessageObj = ImageMessage.parseJsonMessageV2(imageMessage);
        } catch (MalformedURLException malformException) {
            LOGGER.error("parseJson message error", malformException);
            validationStatus.setStatus(false);
            validationStatus.setMessage(malformException.getMessage());
            return validationStatus;
        }
        for (MediaMessageValidator validator : validators) {
            validationStatus = validator.validate(imageMessageObj);
            if (validationStatus.isStatus() == false) {
                return validationStatus;
            }
        }
        return validationStatus;
    }

}
