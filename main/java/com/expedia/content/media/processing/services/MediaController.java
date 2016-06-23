package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;
import com.expedia.content.media.processing.pipeline.reporting.Activity;
import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.LogEntry;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.pipeline.retry.RetryableMethod;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.MediaUpdateDao;
import com.expedia.content.media.processing.services.dao.SKUGroupCatalogItemDao;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.reqres.MediaByDomainIdResponse;
import com.expedia.content.media.processing.services.reqres.MediaGetResponse;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
import com.expedia.content.media.processing.services.util.FileNameUtil;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaReplacement;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import com.expedia.content.media.processing.services.validator.MapMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import expedia.content.solutions.metrics.annotations.Counter;
import expedia.content.solutions.metrics.annotations.Gauge;
import expedia.content.solutions.metrics.annotations.Meter;
import expedia.content.solutions.metrics.annotations.Timer;
import org.apache.commons.lang.StringUtils;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
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
@RestController
public class MediaController extends CommonServiceController {

    private static final String RESPONSE_FIELD_MEDIA_GUID = "mediaGuid";
    private static final String RESPONSE_FIELD_STATUS = "status";
    private static final String ERROR_MESSAGE = "error message";
    private static final String REJECTED_STATUS = "REJECTED";
    private static final String RESPONSE_FIELD_THUMBNAIL_URL = "thumbnailUrl";
    private static final String RESPONSE_FIELD_LCM_MEDIA_ID = "lcmMediaId";
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String GUID_REG = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final String MEDIA_CLOUD_ROUTER_CLIENT_ID = "Media Cloud Router";
    private static final String MEDIA_VALIDATION_ERROR = "validationError";
    private static final String DOMAIN = "domain";
    private static final String DOMAIN_ID = "domainId";
    private static final String DYNAMO_MEDIA_FIELD = "dynamoMedia";
    private static final String NEW_JASON_FIELD = "newJson";
    private static final String IMAGE_MESSAGE_FIELD = "message";
    private static final String REPROCESSING_STATE_FIELD = "processState";
    private static final String REG_EX_NUMERIC = "\\d+";
    private static final String REG_EX_GUID = "[a-z0-9]{8}(-[a-z0-9]{4}){3}-[a-z0-9]{12}";
    private static final String UNAUTHORIZED_USER_MESSAGE = "User is not authorized.";
    private static final String DUPLICATED_STATUS = "DUPLICATE";
    private static final Integer LIVE_COUNT = 1;
    private static final String DEFAULT_VALIDATION_RULES = "DEFAULT";
    private static final Map<String, HttpStatus> STATUS_MAP = new HashMap<>();
    static {
        STATUS_MAP.put(ValidationStatus.NOT_FOUND, NOT_FOUND);
        STATUS_MAP.put(ValidationStatus.ZERO_BYTES, BAD_REQUEST);
        STATUS_MAP.put(ValidationStatus.VALID, OK);
    }

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
    private MediaDao mediaDao;
    @Autowired
    private MediaUpdateDao mediaUpdateDao;
    @Autowired
    private DynamoMediaRepository dynamoMediaRepository;
    @Autowired
    private MediaUpdateProcessor mediaUpdateProcessor;
    @Autowired
    private SKUGroupCatalogItemDao skuGroupCatalogItemDao;

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
            final String userName = "Multisource";
            return processRequest(mediaCommonMessage, requestID, serviceUrl, userName, OK);
        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - messageName={}, JSONMessage=[{}], error=[{}], requestId=[{}] .", serviceUrl, message, ex.getMessage(), requestID, ex);
            return this.buildErrorResponse("JSON request format is invalid. Json message=" + message, serviceUrl, BAD_REQUEST);
        }
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
        final String serviceUrl = MediaServiceUrl.MEDIA_IMAGES.getUrl();
        LOGGER.info("RECEIVED REQUEST - messageName={}, requestId=[{}], JSONMessage=[{}]", serviceUrl, requestID, message);
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final String clientId = auth.getName();
            return processRequest(message, requestID, serviceUrl, clientId, ACCEPTED);
        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - messageName={}, error=[{}], requestId=[{}], JSONMessage=[{}].", serviceUrl, ex.getMessage(), requestID, message, ex);
            return this.buildErrorResponse("JSON request format is invalid. Json message=" + message, serviceUrl, BAD_REQUEST);
        }
    }

    /**
     * web service interface to update media information
     *
     * @param queryId can be lcmMediaId or media GUID
     * @param message JSON message
     * @param headers
     * @return
     * @throws Exception
     */
    @Meter(name = "updateMessageCounter")
    @Timer(name = "updateMessageTimer")
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @RequestMapping(value = "/media/v1/images/{queryId}", method = RequestMethod.PUT)
    public ResponseEntity<String> mediaUpdate(@PathVariable("queryId") final String queryId, @RequestBody final String message,
            @RequestHeader final MultiValueMap<String, String> headers) throws Exception {
        final String requestID = this.getRequestId(headers);
        final String serviceUrl = MediaServiceUrl.MEDIA_IMAGES.getUrl() + "/" + queryId;
        LOGGER.info("RECEIVED update REQUEST - serviceUrl={}, queryId=[{}], requestId=[{}], JSONMessage=[{}]", serviceUrl, queryId, requestID, message);
        try {
            final Map<String, Object> objectMap = new HashMap<>();
            validateAndInitMap(objectMap, queryId, serviceUrl, message, requestID);
            if (objectMap.get(MEDIA_VALIDATION_ERROR) != null) {
                return (ResponseEntity<String>) objectMap.get(MEDIA_VALIDATION_ERROR);
            }
            final String lcmMediaId = (String) objectMap.get(RESPONSE_FIELD_LCM_MEDIA_ID);
            final String domainId = (String) objectMap.get(DOMAIN_ID);
            final Media dynamoMedia = (Media) objectMap.get(DYNAMO_MEDIA_FIELD);

            final String newJson = (String) objectMap.get(NEW_JASON_FIELD);
            final ImageMessage imageMessage = ImageMessage.parseJsonMessage(newJson);
            if (message.contains("active")) {
                return mediaUpdateProcessor.processRequest(imageMessage, lcmMediaId, domainId, dynamoMedia);
            } else {
                final ImageMessage imageMessageNew = removeActiveFromImageMessage(imageMessage);
                return mediaUpdateProcessor.processRequest(imageMessageNew, lcmMediaId, domainId, dynamoMedia);
            }
        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - serviceUrl={}, error=[{}], queryId=[{}], requestId=[{}],JSONMessage=[{}].", serviceUrl, ex.getMessage(), queryId,
                    requestID, message, ex);
            return this.buildErrorResponse("JSON request format is invalid. Json message=" + message, serviceUrl, BAD_REQUEST);
        } catch (Exception ex) {
            LOGGER.error("ERROR when update media -serviceUrl={}, error=[{}], queryId=[{}],requestId=[{}], JSONMessage=[{}].", serviceUrl, ex.getMessage(),
                    queryId, requestID, message, ex);
            return this.buildErrorResponse("update failure with message=" + ex.getMessage(), serviceUrl, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Web services interface to retrieve media information by its GUID.
     *
     * @param mediaGUID The GUID of the requested media.
     * @param headers   Headers of the request.
     * @return The requested media information.
     * @throws Exception Thrown if processing the message fails.
     */
    @Meter(name = "getMediaByGUIDMessageCounter")
    @Timer(name = "getMediaByGUIDMessageTimer")
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @RequestMapping(value = "/media/v1/images/{mediaGUID}", method = RequestMethod.GET)
    @Transactional
    public ResponseEntity<String> getMedia(@PathVariable("mediaGUID") final String mediaGUID, @RequestHeader final MultiValueMap<String, String> headers)
            throws Exception {
        final String requestID = this.getRequestId(headers);
        final String serviceUrl = MediaServiceUrl.MEDIA_IMAGES.getUrl();
        LOGGER.info("RECEIVED REQUEST - messageName={}, requestId=[{}], mediaGUID=[{}]", serviceUrl, requestID, mediaGUID);
        //TODO Once lodging data transfered to media DB the second condition, numeric, will need to be removed.
        if (!mediaGUID.matches(REG_EX_GUID) && !mediaGUID.matches(REG_EX_NUMERIC)) {
            LOGGER.warn("INVALID REQUEST - messageName={}, requestId=[{}], mediaGUID=[{}]", serviceUrl, requestID, mediaGUID);
            return this.buildErrorResponse("Invalid media GUID provided.", serviceUrl, BAD_REQUEST);
        }
        final String dynamoGuid = getGuidByMediaId(mediaGUID);
        if (dynamoGuid != null) {
            LOGGER.info("Media GUID [{}] exists, please use GUID in request. requestId=[{}]", mediaGUID, requestID);
            return this.buildErrorResponse("Media GUID " + dynamoGuid + " exists, please use GUID in request.", serviceUrl, BAD_REQUEST);
        }
        final MediaGetResponse mediaResponse = mediaDao.getMediaByGUID(mediaGUID);
        if (mediaResponse == null) {
            LOGGER.info("Response not found. Provided media GUID does not exist' for requestId=[{}], mediaGUID=[{}]", requestID, mediaGUID);
            return this.buildErrorResponse("Provided media GUID does not exist.", serviceUrl, NOT_FOUND);
        }
        return new ResponseEntity<String>(OBJECT_MAPPER.writeValueAsString(mediaResponse), OK);
    }

    /**
     * Web services interface to retrieve media information by domain name and id.
     *
     * @param domainName           Name of the domain the domain id belongs to.
     * @param domainId             Identification of the domain item the media is required.
     * @param activeFilter         Filter determining what images to return. When true only active are returned. When false only inactive media is returned. When
     *                             all then all are returned. All is set a default.
     * @param derivativeTypeFilter Inclusive filter to use to only return certain types of derivatives. Returns all derivatives if not specified.
     * @param derivativeCategoryFilter Inclusive filter to use to only return certain types of medias. Returns all medias if not specified.
     * @param headers              Headers of the request.
     * @param pageSize             Positive integer to filter the number of media displayed per page. pageSize is inclusive with pageIndex.
     * @param pageIndex            Positive integer to filter the page to display. pageIndex is inclusive with pageSize.
     * @return The list of media data belonging to the domain item.
     * @throws Exception Thrown if processing the message fails.
     */
    @Meter(name = "getMediaByDomainIdMessageCounter")
    @Timer(name = "getMediaByDomainIdMessageTimer")
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @RequestMapping(value = "/media/v1/imagesbydomain/{domainName}/domainId/{domainId}", method = RequestMethod.GET)
    @Transactional
    public ResponseEntity<String> getMediaByDomainId(@PathVariable("domainName") final String domainName, @PathVariable("domainId") final String domainId,
            @RequestParam(value = "pageSize", required = false) final Integer pageSize,
            @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
            @RequestParam(value = "activeFilter", required = false,
                    defaultValue = "all") final String activeFilter,
            @RequestParam(value = "derivativeTypeFilter", required = false) final String derivativeTypeFilter,
            @RequestParam(value = "derivativeCategoryFilter", required = false) final String derivativeCategoryFilter,
            @RequestHeader final MultiValueMap<String, String> headers) throws Exception {
        final String requestID = this.getRequestId(headers);
        final String serviceUrl = MediaServiceUrl.MEDIA_BY_DOMAIN.getUrl();
        LOGGER.info("RECEIVED REQUEST - messageName={}, requestId=[{}], domainName=[{}], domainId=[{}], pageSize=[{}], pageIndex=[{}], activeFilter=[{}], derivativeTypeFilter=[{}]",
                serviceUrl, requestID, domainName, domainId, pageSize, pageIndex, activeFilter, derivativeTypeFilter);
        final ResponseEntity<String> validationResponse = validateMediaByDomainIdRequest(domainName, domainId, activeFilter);
        if (validationResponse != null) {
            LOGGER.warn("INVALID REQUEST - messageName={}, requestId=[{}], domainName=[{}], domainId=[{}], pageSize=[{}], pageIndex=[{}], activeFilter=[{}], derivativeTypeFilter=[{}]",
                    serviceUrl, requestID, domainName, domainId, pageSize, pageIndex, activeFilter, derivativeTypeFilter);
            return validationResponse;
        }
        final MediaByDomainIdResponse response;
        try {
            response = mediaDao.getMediaByDomainId(Domain.findDomain(domainName, true), domainId, activeFilter, derivativeTypeFilter, derivativeCategoryFilter, pageSize, pageIndex);
        } catch (Exception ex) {
            LOGGER.warn(
                    "INVALID REQUEST - messageName={}, requestId=[{}], domainName=[{}], domainId=[{}], pageSize=[{}], pageIndex=[{}], activeFilter=[{}], derivativeTypeFilter=[{}]",
                    serviceUrl, requestID, domainName, domainId, pageSize, pageIndex, activeFilter, derivativeTypeFilter);
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
        return new ResponseEntity<>(OBJECT_MAPPER.writeValueAsString(response), OK);
    }

    /**
     * Sends the metric server a unique value. This is useful to know:
     * 1- The number of active or inactive  instances within a period of time.
     * 2- How long the component was up or down within a period of time.
     *
     * @return Always return a single value.
     */
    @Gauge(name = "isAlive")
    @Counter(name = "isAliveCounter")
    public Integer liveCount() {
        return LIVE_COUNT;
    }

    private void validateAndInitMap(Map<String, Object> objectMap, String queryId, String serviceUrl, String message, String requestID) throws Exception {
        Media dynamoMedia = null;
        if (queryId.matches(GUID_REG)) {
            dynamoMedia = mediaDao.getMediaByGuid(queryId);
            if (dynamoMedia == null) {
                objectMap.put(MEDIA_VALIDATION_ERROR, this.buildErrorResponse("input GUID does not exist in DB", serviceUrl, NOT_FOUND));
                return;
            }
            objectMap.put(RESPONSE_FIELD_LCM_MEDIA_ID, dynamoMedia.getLcmMediaId());
            objectMap.put(DOMAIN_ID, dynamoMedia.getDomainId());
            objectMap.put(DYNAMO_MEDIA_FIELD, dynamoMedia);
        } else if (StringUtils.isNumeric(queryId)) {
            final List<Media> mediaList = mediaDao.getMediaByMediaId(queryId);
            if (!mediaList.isEmpty()) {
                final String guid = mediaList.get(0).getMediaGuid();
                objectMap.put(MEDIA_VALIDATION_ERROR,
                        this.buildErrorResponse("Media GUID " + guid + " exists, please use GUID in request.", serviceUrl, BAD_REQUEST));
                return;
            }
            final LcmMedia lcmMedia = mediaUpdateDao.getMediaByMediaId(Integer.valueOf(queryId));
            if (lcmMedia == null) {
                objectMap.put(MEDIA_VALIDATION_ERROR, this.buildErrorResponse("input mediaId does not exist in DB", serviceUrl, NOT_FOUND));
                return;
            }
            objectMap.put(RESPONSE_FIELD_LCM_MEDIA_ID, queryId);
            objectMap.put(DOMAIN_ID, lcmMedia.getDomainId().toString());

        } else {
            objectMap.put(MEDIA_VALIDATION_ERROR, this.buildErrorResponse("input queryId is invalid", serviceUrl, BAD_REQUEST));
            return;
        }
        final String newJson = appendDomain(message, (String) objectMap.get(DOMAIN_ID));
        objectMap.put(NEW_JASON_FIELD, newJson);
        final String jsonError = validateImageMessage(newJson, "EPCUpdate");
        if (!"[]".equals(jsonError)) {
            LOGGER.error("Returning BAD_REQUEST for serviceUrl={}, queryId=[{}],requestId=[{}], JSONMessage=[{}], Errors=[{}]", serviceUrl, queryId,
                    requestID, message, jsonError);
            objectMap.put(MEDIA_VALIDATION_ERROR, this.buildErrorResponse(jsonError, serviceUrl, BAD_REQUEST));
            return;
        }
        if (mediaCannotBeHidden(dynamoMedia, newJson)){
            objectMap.put(MEDIA_VALIDATION_ERROR, this.buildErrorResponse("Only unpublished media can be hidden", serviceUrl, BAD_REQUEST));
        }
    }

    /**
     *  Verify if a media can be hidden.
     *  An image can be permanently hidden from all messages, including further updates.
     *  This is not applied to published images. only unpublished images (Duplicated and Rejected) can be hidden.
     *
     * @param media Media to verify
     * @param message Incoming update message.
     * @return returns true if the media can be hidden or false if not.
     */
    private Boolean mediaCannotBeHidden(Media media, String message) throws Exception {
        if (media == null) {
            return false;
        }
        final ImageMessage updateMessage = ImageMessage.parseJsonMessage(message);
        final String fileName = media.getFileName();
        final String latestStatus = mediaDao.getLatestStatus(Arrays.asList(fileName)).get(fileName);
        return !(REJECTED_STATUS.equals(latestStatus) || DUPLICATED_STATUS.equals(latestStatus)) && updateMessage.getHidden();
    }

    private ImageMessage removeActiveFromImageMessage(final ImageMessage imageMessage) {
        ImageMessage.ImageMessageBuilder imageMessageBuilder = new ImageMessage.ImageMessageBuilder();
        imageMessageBuilder = imageMessageBuilder.transferAll(imageMessage);
        imageMessageBuilder.active(null);
        return imageMessageBuilder.build();

    }

    private String appendDomain(String message, String domainId) throws Exception {
        final Map<String, Object> jsonMap = JSONUtil.buildMapFromJson(message);
        jsonMap.put(DOMAIN, Domain.LODGING.getDomain());
        jsonMap.put(DOMAIN_ID, domainId);
        return new ObjectMapper().writeValueAsString(jsonMap);
    }


    /**
     * Validates the media by domain id request.
     *
     * @param domainName   Domain to validate.
     * @param activeFilter Active filter to validate.
     * @return Returns a response if the validation fails; null otherwise.
     */
    private ResponseEntity<String> validateMediaByDomainIdRequest(final String domainName, final String domainId, final String activeFilter) {
        if (activeFilter != null && !activeFilter.equalsIgnoreCase("all") && !activeFilter.equalsIgnoreCase("true")
                && !activeFilter.equalsIgnoreCase("false")) {
            return new ResponseEntity<>("Unsupported active filter " + activeFilter, BAD_REQUEST);
        }
        if (Domain.findDomain(domainName, true) == null) {
            return new ResponseEntity<>("Domain not found " + domainName, NOT_FOUND);
        }
        if (!skuGroupCatalogItemDao.skuGroupExists(Integer.parseInt(domainId))) {
            return new ResponseEntity<>("DomainId not found: " + domainId, NOT_FOUND);
        }
        return null;
    }

    /**
     * Common processing between mediaAdd and the AWS portion of aquireMedia. Can be transfered into mediaAdd once aquireMedia is removed.
     *
     * @param message       JSON formated ImageMessage.
     * @param requestID     The id of the request. Used for tracking purposes.
     * @param serviceUrl    URL of the message called.
     * @param clientId      Web service client id.
     * @param successStatus Status to return when successful.
     * @return The response for the service call.
     * @throws Exception Thrown if the message can't be validated or the response can't be serialized.
     */
    @SuppressWarnings({"PMD.PrematureDeclaration"})
    private ResponseEntity<String> processRequest(final String message, final String requestID, final String serviceUrl, final String clientId,
            HttpStatus successStatus) throws Exception {
        final String json = validateImageMessage(message, clientId);
        if (!"[]".equals(json)) {
            LOGGER.warn("Returning bad request for messageName={}, requestId=[{}], JSONMessage=[{}]. Errors=[{}]", serviceUrl, requestID, message, json);
            return this.buildErrorResponse(json, serviceUrl, BAD_REQUEST);
        }
        @SuppressWarnings("CPD-START")
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
        final ValidationStatus fileValidation = verifyUrl(imageMessage.getFileUrl());
        if (!fileValidation.isValid()) {
            switch (fileValidation.getStatus()) {
                case ValidationStatus.NOT_FOUND :
                    LOGGER.info("Response not found. Provided 'fileUrl does not exist' for requestId=[{}], message=[{}]", requestID, message);
                    break;
                case ValidationStatus.ZERO_BYTES :
                    LOGGER.info("Returning bad request. Provided 'file is 0 Bytes' for requestId=[{}], message=[{}]", requestID, message);
                    break;
                default :
                    LOGGER.info("Returning bad request. requestId=[{}], message=[{}]", requestID, message);
                    break;
            }
            return this.buildErrorResponse(fileValidation.getMessage(), serviceUrl, STATUS_MAP.get(fileValidation.getStatus()) == null ? BAD_REQUEST : STATUS_MAP.get(fileValidation.getStatus()));
        }
        @SuppressWarnings("CPD-END")
        final Map<String, Object> messageState = updateImageMessage(imageMessage, requestID, clientId);
        final ImageMessage imageMessageNew = (ImageMessage) messageState.get(IMAGE_MESSAGE_FIELD);
        final Boolean isReprocessing = (Boolean) messageState.get(REPROCESSING_STATE_FIELD);

        final Map<String, String> response = new HashMap<>();
        response.put(RESPONSE_FIELD_MEDIA_GUID, imageMessageNew.getMediaGuid());
        response.put(RESPONSE_FIELD_STATUS, "RECEIVED");
        Thumbnail thumbnail = null;
        if (imageMessageNew.isGenerateThumbnail()) {
            try {
                thumbnail = thumbnailProcessor.createThumbnail(imageMessageNew);
            } catch (Exception e) {
                response.put(RESPONSE_FIELD_STATUS, REJECTED_STATUS);
                response.put(ERROR_MESSAGE, e.getLocalizedMessage());
                return new ResponseEntity<>(OBJECT_MAPPER.writeValueAsString(response), HttpStatus.UNPROCESSABLE_ENTITY);
            }
            response.put(RESPONSE_FIELD_THUMBNAIL_URL, thumbnail.getLocation());
        }
        if (!isReprocessing) {
            dynamoMediaRepository.storeMediaAddMessage(imageMessageNew, thumbnail);
        }
        publishMsg(imageMessageNew);
        LOGGER.info("SUCCESS - messageName={}, requestId=[{}], mediaGuid=[{}], JSONMessage=[{}]", serviceUrl, requestID, imageMessageNew.getMediaGuid(),
                message);
        return new ResponseEntity<>(OBJECT_MAPPER.writeValueAsString(response), successStatus);
    }

    /**
     * Updates the image message for the next step. Must be done before being
     * published to the next work queue.
     *
     * @param imageMessage The incoming image message.
     * @param requestID    The id of the request. Used for tracking purposes.
     * @param clientId     Web service client id.
     * @return A Map contains the updated message with request and other data added
     * and if the file is checked for reprocessing .
     */
    private Map<String, Object> updateImageMessage(final ImageMessage imageMessage, final String requestID, final String clientId) {
        ImageMessage.ImageMessageBuilder imageMessageBuilder = new ImageMessage.ImageMessageBuilder();
        imageMessageBuilder = imageMessageBuilder.transferAll(imageMessage);
        imageMessageBuilder.mediaGuid(UUID.randomUUID().toString());
        final OuterDomain outerDomain = getDomainProviderFromMapping(imageMessage.getOuterDomainData());
        imageMessageBuilder.outerDomainData(outerDomain);
        Boolean isReprocessing = false;
        if (MEDIA_CLOUD_ROUTER_CLIENT_ID.equals(clientId)) {
            isReprocessing = processReplacement(imageMessage, imageMessageBuilder, clientId);
        } else {
            if (imageMessage.getProvidedName() == null) {
                imageMessageBuilder.providedName(resolveProvidedName(imageMessage));
            }
            imageMessageBuilder.fileName(FileNameUtil.resolveFileNameByProvider(imageMessageBuilder.build()));
        }
        final ImageMessage imageMessageNew = imageMessageBuilder.clientId(clientId).requestId(String.valueOf(requestID)).build();
        final Map<String, Object> messageState = new HashMap<>();
        messageState.put(IMAGE_MESSAGE_FIELD, imageMessageNew);
        messageState.put(REPROCESSING_STATE_FIELD, isReprocessing);

        return messageState;
    }

    /**
     * Resolves which fileName should be used as the ProvidedName. If the fileName field does not exist a name
     * is extracted from the FileURL.
     * - Note - this method will only be called on new media sent through MediaAdd.
     * - Note - Media sent through AcquireMedia will already have a providedName when the json
     * message is parsed and Reprocessed Media will never end up in the branch in @updateImageMessage()
     * that calls this method
     *
     * @param imageMessage an imageMessage that does has null for the providedName field
     * @return the fileName to use for the providedName field
     */
    private String resolveProvidedName(final ImageMessage imageMessage) {
        return (imageMessage.getFileName() == null) ?
                FileNameUtil.getFileNameFromUrl(imageMessage.getFileUrl()) :
                imageMessage.getFileName();
    }

    /**
     * This method processes the replacement changes needed on the
     * ImageMessageBuilder for the provided ImageMessage . Reprocessing
     * happens only if the request comes from Media Cloud Router (as clientId)
     * <p>
     * The method will first try to find the media that have the same file name.
     * If multiple, it will choose the best one for replacement. It will finally
     * populate the replacement queryId and GUID on the ImageMessageBuilder.
     * </p>
     *
     * @param imageMessage        Original message received.
     * @param imageMessageBuilder Builder for the new/mutated ImageMessage.
     * @param clientId            Existing in the message header, represents the client (EPC, Media Cloud Router, Multisource, GSO Media Tools)
     * @return returns true if reprocessing and false if not.
     */

    private boolean processReplacement(ImageMessage imageMessage, ImageMessage.ImageMessageBuilder imageMessageBuilder, String clientId ) {
        if (MEDIA_CLOUD_ROUTER_CLIENT_ID.equals(clientId)) {
            final List<Media> mediaList = mediaDao.getMediaByFilename(imageMessage.getFileName());
            final Optional<Media> bestMedia = MediaReplacement
                    .selectBestMedia(mediaList, imageMessage.getOuterDomainData().getDomainId(), imageMessage.getOuterDomainData().getProvider());
            // Replace the GUID and MediaId of the existing Media
            if (bestMedia.isPresent()) {
                final Media media = bestMedia.get();
                final OuterDomain.OuterDomainBuilder domainBuilder = OuterDomain.builder().from(imageMessage.getOuterDomainData());
                domainBuilder.addField(RESPONSE_FIELD_LCM_MEDIA_ID, media.getLcmMediaId());
                imageMessageBuilder.outerDomainData(domainBuilder.build());
                imageMessageBuilder.mediaGuid(media.getMediaGuid());
                imageMessageBuilder.providedName(media.getProvidedName());

                LOGGER.info("This is a replacement: mediaGuid=[{}], filename=[{}], requestId=[{}], lcmMediaId=[{}]", media.getMediaGuid(),
                        imageMessage.getFileName(), imageMessage.getRequestId(), media.getDomainId());
                return true;
            } else {
                final List<LcmMedia> lcmMediaList = mediaDao.getMediaByFilenameInLCM(Integer.valueOf(imageMessage.getOuterDomainData().getDomainId()),imageMessage.getFileName());
                final Optional<LcmMedia> existMedia = lcmMediaList.stream().filter(lcmMedia -> lcmMedia.getActive()).findFirst();
                if(existMedia.isPresent()){
                    final LcmMedia lcmMedia = existMedia.get();
                    final OuterDomain.OuterDomainBuilder domainBuilder = OuterDomain.builder().from(imageMessage.getOuterDomainData());
                    domainBuilder.addField(RESPONSE_FIELD_LCM_MEDIA_ID, lcmMedia.getMediaId().toString());
                    imageMessageBuilder.outerDomainData(domainBuilder.build());
                    LOGGER.info("The replacement information by lcm is:  filename=[{}], requestId=[{}], lcmMediaId=[{}]",
                            imageMessage.getFileName(), imageMessage.getRequestId(), lcmMedia.getMediaId());
                    return true;

                }
                LOGGER.info("Could not find the best media for the filename=[{}] on the list: [{}]. Will create a new GUID.", imageMessage.getFileName(),
                        Joiner.on("; ").join(mediaList));
            }
        }
        return false;
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
     * @param message  input json message.
     * @param clientId Web service client id.
     * @return JSON string contains fileName and error description.
     * @throws Exception when message to ImageMessage and convert java list to json.
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private String validateImageMessage(final String message, final String clientId) throws Exception {
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        final List<MapMessageValidator> defaultValidatorList = mapValidatorList.get(DEFAULT_VALIDATION_RULES);
        final List<MapMessageValidator> validatorList = mapValidatorList.getOrDefault(clientId, defaultValidatorList);
        if (validatorList == defaultValidatorList) {
            LOGGER.warn("There is no validation specific to user: {}. Using default validations.", clientId);
        }
        if (validatorList == null && defaultValidatorList == null) {
            return UNAUTHORIZED_USER_MESSAGE;
        }
        List<Map<String, String>> validationErrorList = null;
        for (final MapMessageValidator mapMessageValidator : validatorList) {
            validationErrorList = mapMessageValidator.validateImages(imageMessageList);
            if (!validationErrorList.isEmpty()) {
                return JSONUtil.convertValidationErrors(validationErrorList);
            }
        }
        return JSONUtil.convertValidationErrors(validationErrorList);
    }

    /**
     * Logs a completed activity and its time. and exepdiaId is appended before the file name
     *
     * @param imageMessage The imageMessage of the file being processed.
     * @param activity     The activity to log.
     */
    private void logActivity(final ImageMessage imageMessage, final Activity activity) throws URISyntaxException {
        final LogEntry logEntry =
                new LogEntry(imageMessage.getFileName(), imageMessage.getMediaGuid(), activity, new Date(), imageMessage.getOuterDomainData().getDomain(),
                        imageMessage.getOuterDomainData().getDomainId(), imageMessage.getOuterDomainData().getDerivativeCategory());
        logActivityProcess.log(logEntry, reporting);
    }

    /**
     * get the domainProvider text from the mapping regardless of case-sensitivity
     * if the exact text is not passed, datamanager fails to find it and defaults it
     * to 1
     *
     * @param outerDomain
     * @return outerDomain with domainProvider replaced by the exact domainProvider from the mapping
     */
    private OuterDomain getDomainProviderFromMapping(OuterDomain outerDomain) {
        final String domainProvider = DomainDataUtil.getDomainProvider(outerDomain.getProvider(), providerProperties);
        return OuterDomain.builder().from(outerDomain).mediaProvider(domainProvider).build();
    }

    /**
     * Retrieve the GUID base on a given LCM mediaId.
     *
     * @param mediaId
     * @return The GUID or null if no media found in dynamo.
     */
    private String getGuidByMediaId(String mediaId){
        if (StringUtils.isNumeric(mediaId)) {
            final List<Media> mediaList = mediaDao.getMediaByMediaId(mediaId);
            if (!mediaList.isEmpty()) {
                 return mediaList.stream().findFirst().get().getMediaGuid();
            }
        }
        return null;
    }
}
