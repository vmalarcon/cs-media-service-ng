package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.domain.ImageTypeComponentPicker;
import com.expedia.content.media.processing.pipeline.retry.RetryableMethod;
import com.expedia.content.media.processing.pipleline.reporting.Activity;
import com.expedia.content.media.processing.pipleline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipleline.reporting.Reporting;
import com.expedia.content.media.processing.pipleline.reporting.sql.MediaProcessLog;
import com.expedia.content.media.processing.services.validator.MediaMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import com.expedia.content.metrics.aspects.annotations.Meter;
import com.expedia.content.metrics.aspects.annotations.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * MediaServiceProcess is called by main class
 */
@Component
public class MediaServiceProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceProcess.class);
    private static final String SQL_PARAM_TEMPLATE = "<Parameter type=\"MediaFileName\" val=\"fileName\"/>";
    private static final String JSON_TAG_STATUS = "status";
    private static final String JSON_TAG_TIME = "time";
    private static final String JSON_TAG_MEDIA_NAME = "mediaName";
    private static final String JSON_TAG_MEDIA_STATUS = "mediaStatuses";
    private static final String JSON_TAG_STATUS_LIST = "statuses";



    private final List<MediaMessageValidator> validators;
    private final RabbitTemplate rabbitTemplate;
    private final ImageTypeComponentPicker<LogActivityProcess> logActivityPicker;
    private final Reporting reporting;
    private final Map<String, String> mediaStatusMap;

    /**
     * @param validators     injected from spring configuration file
     * @param rabbitTemplate jms queue template
     */
    public MediaServiceProcess(List<MediaMessageValidator> validators, RabbitTemplate rabbitTemplate,
            @Qualifier("logActivityPicker") final ImageTypeComponentPicker<LogActivityProcess> logActivityPicker, final Reporting reporting, final Map mediaStatusMap) {
        this.validators = validators;
        this.rabbitTemplate = rabbitTemplate;
        this.logActivityPicker = logActivityPicker;
        this.reporting = reporting;
        this.mediaStatusMap = mediaStatusMap;
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
            rabbitTemplate.convertAndSend(jsonMessage);
            LOGGER.debug("send message to queue done: [{}]", jsonMessage);
            logActivity(message, Activity.MEDIA_MESSAGE_RECEIVED);
        } catch (Exception ex) {
            LOGGER.error("Error publishing message=[{}]", jsonMessage, ex);
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

    public String getMediaStatusList(List<String> fileNameList) throws Exception {
        Map<String, Object> allMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        List mediaStatusList = new ArrayList();
        for (String fileName : fileNameList) {
            Map<String, Object> fileMap = new HashMap<>();
            List subStatusList = new ArrayList();
            String param = SQL_PARAM_TEMPLATE.replace("fileName", fileName);
            List<MediaProcessLog> statusLogList = reporting.findMediaStatus(param);
            if (statusLogList != null) {
                for (MediaProcessLog status : statusLogList) {
                    Map<String, String> objectMap = new HashMap<>();
                    String mappingValue =
                            mediaStatusMap.get(status.getActivityNameAndType()) != null ? mediaStatusMap.get(status.getActivityNameAndType()) :
                                    status.getActivityNameAndType();
                    objectMap.put(JSON_TAG_STATUS, mappingValue);
                    objectMap.put(JSON_TAG_TIME, status.getActivityTime());
                    subStatusList.add(objectMap);
                }
            }
            fileMap.put(JSON_TAG_STATUS_LIST, subStatusList);
            fileMap.put(JSON_TAG_MEDIA_NAME, fileName);
            mediaStatusList.add(fileMap);
        }
        allMap.put(JSON_TAG_MEDIA_STATUS, mediaStatusList);
        return mapper.writeValueAsString(allMap);
    }

}
