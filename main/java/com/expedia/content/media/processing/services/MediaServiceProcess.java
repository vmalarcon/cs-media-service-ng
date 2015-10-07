package com.expedia.content.media.processing.services;

import com.amazonaws.services.sqs.AmazonSQS;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.ImageTypeComponentPicker;
import com.expedia.content.media.processing.pipeline.reporting.Activity;
import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.pipeline.retry.RetryableMethod;
import com.expedia.content.media.processing.services.dao.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.ProcessLogDao;
import com.expedia.content.media.processing.services.util.ActivityMapping;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.validator.MapMessageValidator;
import com.expedia.content.media.processing.services.validator.MediaMessageValidator;
import com.expedia.content.media.processing.services.validator.RequestMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import com.expedia.content.metrics.aspects.annotations.Meter;
import com.expedia.content.metrics.aspects.annotations.Timer;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

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
    private List<RequestMessageValidator> mediaStatusValidatorList;
    private List<ActivityMapping> activityWhiteList;
    private ProcessLogDao processLogDao;
    private Map<String, List<MapMessageValidator>> mapValidatorList;
    private QueueMessagingTemplate messagingTemplate;

    @Value("${media.aws.queue.name}")
    private String awsQueue;

    public MediaServiceProcess(List<MediaMessageValidator> validators, RabbitTemplate rabbitTemplate,
            @Qualifier("logActivityPicker") final ImageTypeComponentPicker<LogActivityProcess> logActivityPicker, final Reporting reporting) {
        this.validators = validators;
        this.rabbitTemplate = rabbitTemplate;
        this.logActivityPicker = logActivityPicker;
        this.reporting = reporting;
    }

    public List<RequestMessageValidator> getMediaStatusValidatorList() {
        return mediaStatusValidatorList;
    }

    public void setMediaStatusValidatorList(
            List<RequestMessageValidator> mediaStatusValidatorList) {
        this.mediaStatusValidatorList = mediaStatusValidatorList;
    }

    public Map<String, List<MapMessageValidator>> getMapValidatorList() {
        return mapValidatorList;
    }

    public void setMapValidatorList(
            Map<String, List<MapMessageValidator>> mapValidatorList) {
        this.mapValidatorList = mapValidatorList;
    }

    public List<ActivityMapping> getActivityWhiteList() {
        return activityWhiteList;
    }

    public void setActivityWhiteList(List<ActivityMapping> activityWhiteList) {
        this.activityWhiteList = activityWhiteList;
    }

    public ProcessLogDao getProcessLogDao() {
        return processLogDao;
    }

    public void setProcessLogDao(ProcessLogDao processLogDao) {
        this.processLogDao = processLogDao;
    }

    @Autowired
    public void setSqsQueueSender(AmazonSQS amazonSqs) {
        messagingTemplate = new QueueMessagingTemplate(amazonSqs);
    }

    public void publish(@RequestBody String payload) {
        messagingTemplate.send(awsQueue, MessageBuilder.withPayload(payload).build());
    }

    public String receive() {
        String payload = (String) messagingTemplate.receive(awsQueue).getPayload();
        LOGGER.debug("Receiving msg: {}", payload);
        return payload;
    }

    /**
     * publish message to jms queue
     * Note that the {@code @Meter} {@code @Timer} {@code @RetryableMethod} annotations introduce aspects from metrics-support and spring-retry
     * modules. The aspects should be applied in order, Metrics being outside (outer) and retry being inside (inner).
     *
     * @param message is the received json format message from main application
     */
    @Meter(name = "publishMessageCounter")
    @Timer(name = "publishMessageTimer")
    @RetryableMethod
    public void publishMsg(ImageMessage message) {
        String jsonMessage = message.toJSONMessage();
        try {
            //rabbitTemplate.convertAndSend(jsonMessage);
            publish(jsonMessage);
            LOGGER.debug("Sending message to queue done : message=[{}]", jsonMessage);
            logActivity(message, Activity.MEDIA_MESSAGE_RECEIVED);
        } catch (Exception ex) {
            LOGGER.error("Error publishing : message=[{}], error=[{}]", jsonMessage, ex.getMessage(), ex);
            throw new RuntimeException("Error publishing message=[" + jsonMessage + "]", ex);
        }
    }

    /**
     * validate whether the message is valid
     * numeric: mediaProviderId, expediaId,categoryId,
     * required: expediaId, fileUrl,imageType, mediaProviderId
     * URL pattern: fileURL, callback
     *
     * @param imageMessage is the received json format message from main application
     * @return ValidationStatus contain two validation status, true-successful,
     * false- validation fail , in false case, a validation message is set in ValidationStatus
     * @throws Exception in case like jms connection is down
     */
    public ValidationStatus validateImage(ImageMessage imageMessage) throws Exception {
        ValidationStatus validationStatus = new ValidationStatus();
        //in case, no validator defined, we make it true.
        validationStatus.setValid(true);
        for (MediaMessageValidator validator : validators) {
            validationStatus = validator.validate(imageMessage);
            if (!validationStatus.isValid()) {
                return validationStatus;
            }
        }
        return validationStatus;
    }

    /**
     * Get validator list by different client, and do validation by rules and DAO validator (later)
     * return the validation error list that combine all of the error result.
     *
     * @param message input json message
     * @param user    web service user, can be "EPC"
     * @return JSON string contains fileName and error description
     * @throws Exception when message to ImageMessage and convert java list to json
     */
    public String validateImageMessage(String message, String user) throws Exception {
        ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
        List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        List<MapMessageValidator> validatorList = mapValidatorList.get(user);
        if (validatorList == null) {
            return "User is not authorized.";
        }
        List<Map<String, String>> validationErrorList = null;
        for (MapMessageValidator mapMessageValidator : validatorList) {
            validationErrorList = mapMessageValidator.validateImages(imageMessageList);
        }
        return JSONUtil.convertValidationErrors(validationErrorList);
    }

    /**
     * Validates the message.
     * In the JSON message, mediaNames is required and it must contain an array of values
     *
     * @param message input json message
     * @return ValidationStatus contain the validation status, {@code true} when successful or
     * {@code false} when the validation fails. When the validation fails a message is also set in the ValidationStatus.
     * @throws Exception when the message is not valid json format.
     */
    public ValidationStatus validateMediaStatus(String message) throws Exception {
        ValidationStatus validationStatus = new ValidationStatus();
        //in case, no validator defined, we make it true.
        validationStatus.setValid(true);
        for (RequestMessageValidator validator : mediaStatusValidatorList) {
            validationStatus = validator.validate(message);
            if (!validationStatus.isValid()) {
                return validationStatus;
            }
        }
        return validationStatus;
    }

    /**
     * Logs a completed activity and its time. and exepdiaId is appended before the file name
     *
     * @param imageMessage The imageMessage of the file being processed.
     * @param activity     The activity to log.
     */
    private void logActivity(ImageMessage imageMessage, Activity activity) throws URISyntaxException {
        URL fileUrl = imageMessage.getFileUrl();
        LogActivityProcess logActivityProcess = logActivityPicker.getImageTypeComponent(imageMessage.getImageType());
        logActivityProcess.log(fileUrl, imageMessage.processingFileName(), activity, new Date(), reporting, imageMessage.getImageType());
    }

    /**
     * query LCM DB to get the media file status.
     *
     * @param fileNameList
     * @return json message than contain status and time
     * @throws Exception
     */
    @Meter(name = "mediaStatusCounter")
    @Timer(name = "mediaStatusTimer")
    @RetryableMethod
    public String getMediaStatusList(List<String> fileNameList) throws Exception {
        List<MediaProcessLog> statusLogList = processLogDao.findMediaStatus(fileNameList);
        Map<String, List<MediaProcessLog>> mapList = new HashMap<>();
        JSONUtil.divideStatusListToMap(statusLogList, mapList, fileNameList.size());
        return JSONUtil.generateJsonByProcessLogList(mapList, fileNameList, activityWhiteList);
    }

}
