package com.expedia.content.media.processing.services;

import com.amazonaws.util.StringUtils;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;
import com.expedia.content.media.processing.pipeline.reporting.Activity;
import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.LogEntry;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.pipeline.retry.RetryableMethod;
import com.expedia.content.media.processing.services.dao.Media;
import com.expedia.content.media.processing.services.dao.MediaDAO;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import com.expedia.content.media.processing.services.util.RestClient;
import com.expedia.content.media.processing.services.util.RouterUtil;
import com.expedia.content.media.processing.services.validator.HTTPValidator;
import com.expedia.content.media.processing.services.validator.MapMessageValidator;
import com.expedia.content.media.processing.services.validator.MediaReplacement;
import com.expedia.content.media.processing.services.validator.S3Validator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import expedia.content.solutions.metrics.annotations.Meter;
import expedia.content.solutions.metrics.annotations.Timer;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

/**
 * Web service controller for media resources.
 */
@Component
@RestController
public class MediaController extends CommonServiceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private RestClient mediaServiceClient;
    @Autowired
    private RouterUtil routerUtil;
    @Value("${media.router.providers}")
    private String providerRouters;
    @Resource(name = "providerProperties")
    private Properties providerProperties;
    @Autowired
    private LogActivityProcess logActivityProcess;
    @Autowired
    private Reporting reporting;
    @Value("#{imageMessageValidators}")
    private Map<String, List<MapMessageValidator>> mapValidatorList;
    @Autowired
    private QueueMessagingTemplate messagingTemplate;
    @Value("${media.aws.collector.queue.name}")
    private String publishQueue;
    @Autowired
    private ThumbnailProcessor thumbnailProcessor;
    @Autowired
    private MediaDAO mediaDAO;

    /**
     * web service interface to consume media message.
     * Note that the {@code @Meter} {@code @Timer} {@code @Retryable} annotations introduce aspects from metrics-support and spring-retry
     * modules. The aspects should be applied in order, Metrics being outside (outer) and retry being inside (inner).
     *
     * @param message is json format media message,fileUrl and expedia is required.
     * @return ResponseEntity Standard spring response object.
     * @throws Exception Thrown if processing the message fails.
     */
    @Meter(name = "acquireMessageCounter")
    @Timer(name = "acquireMessageTimer")
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "rawtypes"})
    @RequestMapping(value = "/acquireMedia", method = RequestMethod.POST)
    @Deprecated
    public ResponseEntity<String> acquireMedia(@RequestBody final String message, @RequestHeader MultiValueMap<String, String> headers) throws Exception {
        final String requestID = this.getRequestId(headers);
        final String serviceUrl = MediaServiceUrl.ACQUIRE_MEDIA.getUrl();
        LOGGER.info("RECEIVED REQUEST - messageName={}, JSONMessage=[{}], requestId=[{}]", serviceUrl, message, requestID);
        try {
            final ImageMessage imageMessageOld = ImageMessage.parseJsonMessage(message);
            final Map messageMap = JSONUtil.buildMapFromJson(message);

            final String mediaCommonMessage = JSONUtil.convertToCommonMessage(imageMessageOld, messageMap, providerProperties);
            LOGGER.info("converted to - common message =[{}]", mediaCommonMessage);
            final ImageMessage imageMessageCommon = ImageMessage.parseJsonMessage(mediaCommonMessage);
            final String routeName = getRouteNameByProvider(imageMessageCommon);
            final boolean sendToAWS = routerUtil.routeAWSByPercentage(routeName);

            LOGGER.debug("send message to AWS {}", sendToAWS);
            // new mediaCommon Message.
            if (sendToAWS) {
                // reuse current validation logic
                final String userName = "Multisource";
                return processRequest(mediaCommonMessage, requestID, serviceUrl, userName, OK);
            } else {
                final String response = mediaServiceClient.callMediaService(message);
                LOGGER.info("SUCCESS send message to media service  - JSONMessage=[{}], response=[{}], requestId=[{}]", message, response, requestID);
                return new ResponseEntity<>("OK,message sent to mpp media service successfully.", OK);
            }

        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - messageName={}, JSONMessage=[{}], error=[{}], requestID=[{}] .", serviceUrl, message, ex, requestID);
            return this.buildErrorResponse("JSON request format is invalid. Json message=" + message, serviceUrl, BAD_REQUEST);
        }
    }

    @Deprecated
    private String getRouteNameByProvider(ImageMessage imageMessage) {
        return (imageMessage.getOuterDomainData() != null && providerRouters.contains(imageMessage.getOuterDomainData().getProvider())) ? imageMessage
                .getOuterDomainData().getProvider() : RouterUtil.DEFAULT_ROUTER_NAME;
    }

    /**
     * Web service interface to push a media file into the media processing pipeline.
     *
     * @param message JSON formated ImageMessage.
     * @return headers Request headers.
     * @throws Exception Thrown if processing the message fails.
     * @see com.expedia.content.media.processing.pipeline.domain.ImageMessage
     */
    @Meter(name = "addMessageCounter")
    @Timer(name = "addMessageTimer")
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @RequestMapping(value = "/media/v1/images", method = RequestMethod.POST)
    public ResponseEntity<String> mediaAdd(@RequestBody final String message,
                                           @RequestHeader final MultiValueMap<String, String> headers) throws Exception {
        final String requestID = this.getRequestId(headers);
        final String serviceUrl = MediaServiceUrl.MEDIA_ADD.getUrl();
        LOGGER.info("RECEIVED REQUEST - messageName={}, requestId=[{}], JSONMessage=[{}]", serviceUrl, requestID, message);
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final String clientId = auth.getName();
            return processRequest(message, requestID, serviceUrl, clientId, ACCEPTED);
        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - messageName={}, error=[{}], requestId=[{}], JSONMessage=[{}].", serviceUrl, ex, requestID, message);
            return this.buildErrorResponse("JSON request format is invalid. Json message=" + message, serviceUrl, BAD_REQUEST);
        }
    }

    /**
     * Common processing between mediaAdd and the AWS portion of aquireMedia. Can be transfered into mediaAdd once aquireMedia is removed.
     * 
     * @param message JSON formated ImageMessage.
     * @param requestID The id of the request. Used for tracking purposes.
     * @param serviceUrl URL of the message called.
     * @param clientId Web service client id.
     * @param successStatus Status to return when successful.
     * @return The response for the service call.
     * @throws Exception Thrown if the message can't be validated or the response can't be serialized.
     */
    private ResponseEntity<String> processRequest(final String message, final String requestID, final String serviceUrl, final String clientId, HttpStatus successStatus) throws Exception {
        final String json = validateImageMessage(message, clientId);
        if (!"[]".equals(json)) {
            LOGGER.warn("Returning BAD_REQUEST for messageName={}, requestId=[{}], JSONMessage=[{}]. Errors=[{}]",
                    serviceUrl, requestID, message, json);
            return this.buildErrorResponse(json, serviceUrl, BAD_REQUEST);
        }
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);

        final boolean fileExists = verifyExistence(imageMessage);
        if (!fileExists) {
            LOGGER.info("Response bad request provided 'fileUrl does not exist' for requestId=[{}], message=[{}]", requestID, message);
            return this.buildErrorResponse("Provided fileUrl does not exist.", serviceUrl, NOT_FOUND);
        }

        final ImageMessage imageMessageNew = updateImageMessage(imageMessage, requestID, clientId);

        final Map<String, String> response = new HashMap<>();
        response.put("mediaGuid", imageMessageNew.getMediaGuid());
        response.put("status", "RECEIVED");
        if (imageMessageNew.isGenerateThumbnail()) {
            response.put("thumbnailUrl", thumbnailProcessor.createThumbnail(imageMessageNew.getFileUrl(), imageMessageNew.getMediaGuid(),
                    imageMessageNew.getOuterDomainData().getDomain().getDomain(), imageMessageNew.getOuterDomainData().getDomainId()));
        }

        publishMsg(imageMessageNew);
        LOGGER.info("SUCCESS - messageName={}, requestId=[{}], mediaGuid=[{}], JSONMessage=[{}]", serviceUrl, requestID, imageMessageNew.getMediaGuid(), message);
        return new ResponseEntity<>(OBJECT_MAPPER.writeValueAsString(response), successStatus);
    }

    /**
     * Updates the image message for the next step. Must be done before being published to the next work queue.
     * 
     * @param imageMessage The incoming image message.
     * @param requestID The id of the request. Used for tracking purposes.
     * @param clientId Web service client id.
     * @return The updated message with request and other data added.
     */
    private ImageMessage updateImageMessage(final ImageMessage imageMessage, final String requestID, final String clientId) {
        ImageMessage.ImageMessageBuilder imageMessageBuilder = new ImageMessage.ImageMessageBuilder();
        imageMessageBuilder = imageMessageBuilder.transferAll(imageMessage);
        if (imageMessage.getFileName() == null) {
            final String fileNameFromFileUrl =
                    FilenameUtils.getBaseName(imageMessage.getFileUrl()) + "." + FilenameUtils.getExtension(imageMessage.getFileUrl());
            imageMessageBuilder.fileName(StringUtils.isNullOrEmpty(imageMessage.getFileName()) ? fileNameFromFileUrl : imageMessage.getFileName());
        }
        imageMessageBuilder.mediaGuid(UUID.randomUUID().toString());
        if (MediaReplacement.isReplacement(imageMessage)) {
            // This will update the GUID to the old one.
            processReplacement(imageMessage, imageMessageBuilder);
        }
        return imageMessageBuilder.clientId(clientId).requestId(String.valueOf(requestID)).build();
    }

    /**
     * This method processes the replacement changes needed on the ImageMessageBuilder for the provided ImageMessage.
     *
     * <p>The method will first try to find the media that have the same file name. If multiple, it will choose the
     * best one for replacement. It will finally populate the replacement mediaId and GUID on the ImageMessageBuilder.</p>
     *
     * @param imageMessage Original message received.
     * @param imageMessageBuilder Builder for the new/mutated ImageMessage.
     */
    private void processReplacement(ImageMessage imageMessage, ImageMessage.ImageMessageBuilder imageMessageBuilder) {
        LOGGER.info("This is a replacement: mediaGuid=[{}], filename=[{}], requestId=[{}]",
                imageMessage.getMediaGuid(), imageMessage.getFileName(), imageMessage.getRequestId());
        final List<Media> mediaList = mediaDAO.getMediaByFilename(imageMessage.getFileName());
        final Optional<Media> bestMedia = MediaReplacement.selectBestMedia(mediaList);
        // Replace the GUID and MediaId of the existing Media
        if (bestMedia.isPresent()) {
            final Media media = bestMedia.get();
            final OuterDomain.OuterDomainBuilder domainBuilder = OuterDomain.builder().from(imageMessage.getOuterDomainData());
            domainBuilder.addField("lcmMediaId", media.getDomainId());
            imageMessageBuilder.outerDomainData(domainBuilder.build());
            imageMessageBuilder.mediaGuid(media.getMediaGuid());
            LOGGER.info("The replacement information is: mediaGuid=[{}], filename=[{}], requestId=[{}], lcmMediaId=[{}]",
                    media.getMediaGuid(), imageMessage.getFileName(), imageMessage.getRequestId(), media.getDomainId());
        } else {
            LOGGER.warn("Could not find the best media for the filename=[{}] on the list: [{}]. Will create a new GUID.",
                    imageMessage.getFileName(), Joiner.on("; ").join(mediaList));
        }
    }


    /**
     * Verifies if the file exists in an S3 bucket or is available in HTTP.
     * @param imageMessage Incoming image message.
     * @return {@code true} if the file exists; {@code false} otherwise.
     */
    private boolean verifyExistence(final ImageMessage imageMessage) {
        final String fileUrl = imageMessage.getFileUrl();
        if (fileUrl.startsWith(S3Validator.S3_PREFIX)) {
            return S3Validator.checkFileExists(imageMessage.getFileUrl());
        } else {
            return HTTPValidator.checkFileExists(imageMessage.getFileUrl());
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
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    @RetryableMethod
    private void publishMsg(final ImageMessage message) {
        final String jsonMessage = message.toJSONMessage();
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
     * Get validator list by different client, and do validation by rules and DAO validator (later)
     * return the validation error list that combine all of the error result.
     *
     * @param message input json message.
     * @param clientId Web service client id.
     * @return JSON string contains fileName and error description.
     * @throws Exception when message to ImageMessage and convert java list to json.
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private String validateImageMessage(final String message, final String clientId) throws Exception {
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        final List<MapMessageValidator> validatorList = mapValidatorList.get(clientId);
        if (validatorList == null) {
            return "User is not authorized.";
        }
        List<Map<String, String>> validationErrorList = null;
        for (final MapMessageValidator mapMessageValidator : validatorList) {
            validationErrorList = mapMessageValidator.validateImages(imageMessageList);
        }
        return JSONUtil.convertValidationErrors(validationErrorList);
    }

    /**
     * Logs a completed activity and its time. and exepdiaId is appended before the file name
     *
     * @param imageMessage The imageMessage of the file being processed.
     * @param activity The activity to log.
     */
    private void logActivity(final ImageMessage imageMessage, final Activity activity) throws URISyntaxException {
        final LogEntry logEntry =
                new LogEntry(imageMessage.getFileName(), imageMessage.getMediaGuid(), activity, new Date(), imageMessage.getOuterDomainData().getDomain(),
                             imageMessage.getOuterDomainData().getDomainId(), imageMessage.getOuterDomainData().getDerivativeCategory());
        logActivityProcess.log(logEntry, reporting);
    }

}
