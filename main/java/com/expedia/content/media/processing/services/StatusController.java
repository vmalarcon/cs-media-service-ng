package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.pipeline.retry.RetryableMethod;
import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.dao.ProcessLogDao;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.util.ActivityMapping;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import com.expedia.content.media.processing.services.util.RequestMessageException;
import com.expedia.content.media.processing.services.validator.RequestMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import expedia.content.solutions.metrics.annotations.Meter;
import expedia.content.solutions.metrics.annotations.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Web service controller for media status.
 */
@Component
@RestController
public class StatusController extends CommonServiceController {

    private static final FormattedLogger LOGGER = new FormattedLogger(StatusController.class);

    @Autowired
    private List<RequestMessageValidator> mediaStatusValidatorList;
    @Autowired
    private List<ActivityMapping> activityWhiteList;
    @Autowired
    private ProcessLogDao processLogDao;
    @Value("${cs.poke.hip-chat.room}")
    private String hipChatRoom;
    @Autowired
    private Poker poker;

    /**
     * Web service interface to get the latest media file process status.
     *
     * @param message JSON formatted message, "mediaNames", contains an array of media file names.
     * @return ResponseEntity Standard spring response object.
     * @throws Exception Thrown if processing the message fails.
     */
    @SuppressWarnings({"rawtypes", "unchecked", "PMD.SignatureDeclareThrowsException"})
    @Meter(name = "mediaLatestStatusCounter")
    @Timer(name = "mediaLatestStatusTimer")
    @RequestMapping(value = "/media/v1/lateststatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Transactional
    public ResponseEntity getMediaLatestStatus(@RequestBody final String message, @RequestHeader MultiValueMap<String, String> headers) throws Exception {
        final String requestID = getRequestId(headers);
        LOGGER.info("RECEIVED LATEST STATUS REQUEST ServiceUrl={} RequestId={} RequestMessage={}",
                MediaServiceUrl.MEDIA_STATUS.getUrl(), requestID, message);
        String jsonResponse = null;
        try {
            final ValidationStatus validationStatus = validateMediaStatus(message);
            if (!validationStatus.isValid()) {
                return buildErrorResponse(validationStatus.getMessage(), MediaServiceUrl.MEDIA_STATUS.getUrl(), BAD_REQUEST);
            }
            final Map<String, Object> map = JSONUtil.buildMapFromJson(message);
            jsonResponse = getMediaStatusList((List<String>) map.get("mediaNames"));
            LOGGER.info("RESPONSE ServiceUrl={} RequestId={} ResponseMessage={}",
                    MediaServiceUrl.MEDIA_STATUS.getUrl(), requestID, jsonResponse);
        } catch (RequestMessageException ex) {
            LOGGER.error(ex, "ERROR ServiceUrl={} RequestId={} RequestMessage={} ErrorMessage={}",
                    MediaServiceUrl.MEDIA_STATUS.getUrl(), requestID, message, ex.getMessage());
            return buildErrorResponse(ex.getMessage(), MediaServiceUrl.MEDIA_STATUS.getUrl(), BAD_REQUEST);
        } catch (Exception ex) {
            LOGGER.error(ex, "ERROR ServiceUrl={} RequestId={} RequestMessage={} ErrorMessage={}",
                    MediaServiceUrl.MEDIA_STATUS.getUrl(), requestID, message, ex.getMessage());
            poker.poke("Media Services failed to process a getMediaLatestStatus request - RequestId: " + requestID, hipChatRoom,
                    message, ex);
            throw ex;
        }
        return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
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
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public ValidationStatus validateMediaStatus(final String message) throws Exception {
        ValidationStatus validationStatus = new ValidationStatus();
        // in case, no validator defined, we make it true.
        validationStatus.setValid(true);
        for (final RequestMessageValidator validator : mediaStatusValidatorList) {
            validationStatus = validator.validate(message);
            if (!validationStatus.isValid()) {
                return validationStatus;
            }
        }
        return validationStatus;
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
    private String getMediaStatusList(final List<String> fileNameList) {
        final List<MediaProcessLog> statusLogList = processLogDao.findMediaStatus(fileNameList);
        final Map<String, List<MediaProcessLog>> mapList = new HashMap<>();
        JSONUtil.divideStatusListToMap(statusLogList, mapList, fileNameList.size());
        return JSONUtil.generateJsonByProcessLogList(mapList, fileNameList, activityWhiteList);
    }

}
