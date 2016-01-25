package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.reporting.Activity;
import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.LogEntry;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.pipeline.retry.RetryableMethod;
import com.expedia.content.media.processing.services.dao.Category;
import com.expedia.content.media.processing.services.dao.DomainNotFoundException;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.ProcessLogDao;
import com.expedia.content.media.processing.services.util.ActivityMapping;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.validator.MapMessageValidator;
import com.expedia.content.media.processing.services.validator.MediaMessageValidator;
import com.expedia.content.media.processing.services.validator.RequestMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import expedia.content.solutions.metrics.annotations.Meter;
import expedia.content.solutions.metrics.annotations.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MediaServiceProcess is called by main class
 */
@Component
public class MediaServiceProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceProcess.class);

    private final List<MediaMessageValidator> validators;
    private final RabbitTemplate rabbitTemplate;
    private final LogActivityProcess logActivityProcess;
    private final Reporting reporting;
    private List<RequestMessageValidator> mediaStatusValidatorList;
    private List<ActivityMapping> activityWhiteList;
    private ProcessLogDao processLogDao;
    private MediaDomainCategoriesDao mediaDomainCategoriesDao;
    private Map<String, List<MapMessageValidator>> mapValidatorList;
    private QueueMessagingTemplate messagingTemplate;

    @Value("${media.aws.collector.queue.name}")
    private String publishQueue;

    public MediaServiceProcess(List<MediaMessageValidator> validators, RabbitTemplate rabbitTemplate,
            final LogActivityProcess logActivityProcess, final Reporting reporting) {
        this.validators = validators;
        this.rabbitTemplate = rabbitTemplate;
        this.logActivityProcess = logActivityProcess;
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

    public MediaDomainCategoriesDao getMediaDomainCategoriesDao() {
        return mediaDomainCategoriesDao;
    }

    public void setMediaDomainCategoriesDao(MediaDomainCategoriesDao mediaDomainCategoriesDao) {
        this.mediaDomainCategoriesDao = mediaDomainCategoriesDao;
    }

    public void setMessagingTemplate(QueueMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * publish message to jms queue
     * Note that the {@code @Meter} {@code @Timer} {@code @RetryableMethod} annotations introduce aspects from metrics-support and spring-retry
     * modules. The aspects should be applied in order, Metrics being outside (outer) and retry being inside (inner).
     *
     * @param message is ImageMessage converted from input json message
     * @param messageContent is the received json format message from main application
     */
    @Meter(name = "publishCommonMessageCounter")
    @Timer(name = "publishCommonMessageTimer")
    @RetryableMethod
    public void publishMsg(ImageMessage message, String messageContent) {
        try {
            messagingTemplate.send(publishQueue, MessageBuilder.withPayload(messageContent).build());
            LOGGER.info("Sending message to sqs queue done : message=[{}] ", messageContent);
            logActivity(message, Activity.MEDIA_MESSAGE_RECEIVED);
        } catch (Exception ex) {
            LOGGER.error("Error publishing : message=[{}], error=[{}]", messageContent, ex.getMessage(), ex);
            throw new RuntimeException("Error publishing message=[" + messageContent + "]", ex);
        }
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
            messagingTemplate.send(publishQueue, MessageBuilder.withPayload(jsonMessage).build());
            LOGGER.info("Sending message to queue done : message=[{}] ", jsonMessage);
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

    public String validateDomainCategoriesRequest(String user) {
        List<MapMessageValidator> validatorList = mapValidatorList.get(user);
        if (validatorList == null) {
            return "User is not authorized.";
        }
        return "OK";
    }

    /**
     * Logs a completed activity and its time. and exepdiaId is appended before the file name
     *
     * @param imageMessage The imageMessage of the file being processed.
     * @param activity     The activity to log.
     */
    private void logActivity(ImageMessage imageMessage, Activity activity) throws URISyntaxException {
        LogEntry logEntry = new LogEntry(
                imageMessage.getFileName(),
                imageMessage.getMediaGuid(),
                activity,
                new Date(),
                imageMessage.getOuterDomainData().getDomain(),
                imageMessage.getOuterDomainData().getDomainId(),
                imageMessage.getOuterDomainData().getDerivativeCategory());
        logActivityProcess.log(logEntry, reporting);
    }

    /**
     * query LCM DB to get the media file status.
     *
     * @param fileNameList
     * @return json message that contain status and time
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

    /**
     * query LCM DB to get the Categories of a Domain
     *
     * @param domain    The domain to query
     * @param localeId  The localization Id to query by
     * @return  json message of Categories for the specified Domain and LocaleId
     * @throws DomainNotFoundException
     */
    public String getDomainCategories(String domain, String localeId) throws DomainNotFoundException {
        List<Category> domainCategories = mediaDomainCategoriesDao.getMediaCategoriesWithSubCategories(domain, localeId);
        return JSONUtil.generateJsonByCategoryList(domainCategories, domain);
    }

}
