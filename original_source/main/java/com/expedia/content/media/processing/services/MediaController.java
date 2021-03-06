package com.expedia.content.media.processing.services;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;
import com.expedia.content.media.processing.pipeline.kafka.KafkaCommonPublisher;
import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.LogEntry;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBLodgingReferenceHotelIdDao;
import com.expedia.content.media.processing.services.exception.MediaNotFoundException;
import com.expedia.content.media.processing.services.exception.PaginationValidationException;
import com.expedia.content.media.processing.services.reqres.MediaByDomainIdResponse;
import com.expedia.content.media.processing.services.reqres.MediaGetResponse;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import com.expedia.content.media.processing.services.util.ValidatorUtil;
import com.expedia.content.media.processing.services.validator.MapMessageValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import expedia.content.solutions.metrics.annotations.Counter;
import expedia.content.solutions.metrics.annotations.Gauge;
import expedia.content.solutions.metrics.annotations.Meter;
import expedia.content.solutions.metrics.annotations.Timer;

/**
 * Web service controller for media resources.
 */
@RestController
@EnableScheduling
// These suppressions are needed for the validateMediaByDomainIdRequest() method.
@SuppressWarnings({"PMD.NPathComplexity","PMD.CyclomaticComplexity"})
public class MediaController extends CommonServiceController {

    private static final FormattedLogger LOGGER = new FormattedLogger(MediaController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String UNAUTHORIZED_USER_MESSAGE = "User is not authorized.";
    private static final String DOMAIN = "domain";
    private static final String DOMAIN_ID = "domainId";
    private static final String REG_EX_GUID = "[a-z0-9]{8}(-[a-z0-9]{4}){3}-[a-z0-9]{12}";
    private static final String DUPLICATED_STATUS = "DUPLICATE";
    private static final String DEFAULT_VALIDATION_RULES = "DEFAULT";
    private static final String REJECTED_STATUS = "REJECTED";
    private static final Integer LIVE_COUNT = 1;
    private static final long ONE_HOUR = 3600 * 1000;
    private static final Map<String, HttpStatus> STATUS_MAP = new HashMap<>();
    static {
        STATUS_MAP.put(ValidationStatus.NOT_FOUND, NOT_FOUND);
        STATUS_MAP.put(ValidationStatus.ZERO_BYTES, BAD_REQUEST);
        STATUS_MAP.put(ValidationStatus.INVALID, BAD_REQUEST);
        STATUS_MAP.put(ValidationStatus.VALID, OK);
    }

    @Value("${cs.poke.hip-chat.room}")
    private String hipChatRoom;
    @Value("${kafka.imagemessage.topic}")
    private String imageMessageTopic;
    @Value("${kafka.imagemessage.topic.retry}")
    private String imageMessageRetryTopic;
    @Value("${media.aws.processlog.queue.name}")
    private String mediaProcessLogQueue;
    private final Map<String, List<MapMessageValidator>> mapValidatorList;
    private final LogActivityProcess logActivityProcess;
    private final Reporting reporting;
    private final QueueMessagingTemplate messagingTemplate;
    private final MediaDao mediaDao;
    private final MediaDBLodgingReferenceHotelIdDao lodgingReferenceHotelIdDao;
    private final KafkaCommonPublisher kafkaCommonPublisher;
    private final MediaUpdateProcessor mediaUpdateProcessor;
    private final MediaGetProcessor mediaGetProcessor;
    private final MediaAddProcessor mediaAddProcessor;
    private final Poker poker;

    @Autowired
    public MediaController(@Value("#{imageMessageValidators}") Map<String, List<MapMessageValidator>> mapValidatorList, LogActivityProcess logActivityProcess, Reporting reporting,
                           QueueMessagingTemplate messagingTemplate, MediaDao mediaDao, MediaDBLodgingReferenceHotelIdDao lodgingReferenceHotelIdDao,
                           KafkaCommonPublisher kafkaCommonPublisher, MediaUpdateProcessor mediaUpdateProcessor, MediaGetProcessor mediaGetProcessor,
                           MediaAddProcessor mediaAddProcessor, Poker poker) {
        this.mapValidatorList = mapValidatorList;
        this.logActivityProcess = logActivityProcess;
        this.reporting = reporting;
        this.messagingTemplate = messagingTemplate;
        this.mediaDao = mediaDao;
        this.lodgingReferenceHotelIdDao = lodgingReferenceHotelIdDao;
        this.kafkaCommonPublisher = kafkaCommonPublisher;
        this.mediaUpdateProcessor = mediaUpdateProcessor;
        this.mediaGetProcessor = mediaGetProcessor;
        this.mediaAddProcessor = mediaAddProcessor;
        this.poker = poker;
    }

    /**
     * Web service interface to push a media file into the media processing pipeline.
     *
     * @param message JSON formatted ImageMessage.
     * @param headers Request headers.
     * @return A Success ResponseEntity if all processes are successful, otherwise and Error ResponseEntity is returned.
     * @throws Exception Thrown if processing the message fails.
     * @see ImageMessage
     */
    @Meter(name = "addMessageCounter")
    @Timer(name = "addMessageTimer")
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @RequestMapping(value = "/media/v1/images", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.POST)
    public ResponseEntity<String> mediaAdd(@RequestBody final String message, @RequestHeader final MultiValueMap<String,String> headers) throws Exception {
        final Date timeReceived = new Date();
        final String clientID = getClientId();
        final String requestID = verifyRequestId(headers, true);
        final String serviceUrl = MediaServiceUrl.MEDIA_IMAGES.getUrl();
        LOGGER.info("RECEIVED ADD REQUEST ServiceUrl={} ClientId={} RequestId={} JSONMessage={}", serviceUrl, clientID, requestID, message);
        try {
            final Optional<ResponseEntity<String>> errorResponse = validateMediaAddRequest(message, requestID, clientID, serviceUrl);
            if (errorResponse.isPresent()) {
                return errorResponse.get();
            }
            return mediaAddProcessor.processRequest(message, requestID, serviceUrl, clientID, ACCEPTED, timeReceived);
        } catch (ImageMessageException ex) {
            final ResponseEntity<String> errorResponse = buildErrorResponse("JSON request format is invalid. Json message=" + message, serviceUrl, BAD_REQUEST);
            LOGGER.error(ex, "ERROR ResponseStatus={} ResponseBody={} ServiceUrl={} ClientId={} RequestId={} ErrorMessage={}",
                    Arrays.asList(errorResponse.getStatusCode().toString(), errorResponse.getBody(), serviceUrl, clientID, requestID, ex.getMessage()), message);
            return errorResponse;
        } catch (Exception ex) {
            LOGGER.error(ex, "ERROR ServiceUrl={} ClientId={} RequestId={} ErrorMessage={}", Arrays.asList(serviceUrl, clientID, requestID, ex.getMessage()), message);
            poker.poke("Media Services failed to process a mediaAdd request - RequestId: " + requestID, hipChatRoom,
                    message, ex);
            throw ex;
        }
    }

    /**
     * Retrieves the RequestId from the http request headers. It creates one if none is provided.
     *
     * @param headers HTTP request headers
     * @param warnIfMissing raises a WARN level log if the requestId is missing, otherwise just INFO.
     * @return RequestId from the headers or a new RequestId if none could be found in the headers.
     */
    private static String verifyRequestId(MultiValueMap<String,String> headers, boolean warnIfMissing) {
        String requestID = getRequestId(headers);
        if (!ValidatorUtil.isValidUUID(requestID)) {
            requestID = UUID.randomUUID().toString();
            if (warnIfMissing) {
                LOGGER.warn("Creating RequestId={}", requestID);
            } else {
                LOGGER.info("Creating RequestId={}", requestID);
            }
        }
        return requestID;
    }

    /**
     * Validates a Media Add Request. If the request is valid null is returned, otherwise an Error ResponseEntity is returned.
     *
     * @param message The mediaAdd JSON request.
     * @param requestId The requestId sent in the request headers.
     * @param clientId The clientId sent in the request headers.
     * @param serviceUrl The MediaAdd service url.
     * @return an Error ResponseEntity if the request is not valid, null otherwise.
     * @throws Exception validating the image message can throw an exception.
     */
    private Optional<ResponseEntity<String>> validateMediaAddRequest(String message, String requestId, String clientId, String serviceUrl) throws Exception {
        LOGGER.info("Validation of image: RequestId={}", Arrays.asList(requestId), message);
        final String json = validateImageMessage(message, clientId);
        if (!"[]".equals(json)) {
            LOGGER.warn("Returning bad request ServiceUrl={} ClientId={} RequestId={} ErrorMessage={}", Arrays.asList(serviceUrl, clientId, requestId, json), message);
            return Optional.of(buildErrorResponse(json, serviceUrl, BAD_REQUEST));
        }
        @SuppressWarnings("CPD-START")
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
        final ValidationStatus fileValidation = verifyUrl(imageMessage.getFileUrl());
        if (!fileValidation.isValid()) {
            switch (fileValidation.getStatus()) {
                case ValidationStatus.NOT_FOUND:
                    LOGGER.info("NOT FOUND Reason=\"Provided 'fileUrl does not exist'\" ServiceUrl={} ClientId={} RequestId={}",
                            Arrays.asList(serviceUrl, clientId, requestId), message);
                    break;
                case ValidationStatus.ZERO_BYTES:
                    LOGGER.info("BAD REQUEST Reason=\"Provided 'file is 0 Bytes'\" ServiceUrl={} ClientId={} RequestId={}",
                            Arrays.asList(serviceUrl, clientId, requestId), message);
                    break;
                default:
                    LOGGER.info("BAD REQUEST ServiceUrl={} ClientId={} RequestId={}",
                            Arrays.asList(serviceUrl, clientId, requestId), message);
                    break;
            }
            return Optional.of(buildErrorResponse(fileValidation.getMessage(), serviceUrl, STATUS_MAP.get(fileValidation.getStatus()) == null ? BAD_REQUEST : STATUS_MAP.get(fileValidation.getStatus())));
        }
        return Optional.empty();
    }

    /**
     * web service interface to update media information
     *
     * @param queryId can be lcmMediaId or media GUID
     * @param message JSON message
     * @param headers Request headers.
     * @return A Success ResponseEntity if all processes are successful, otherwise and Error ResponseEntity is returned.
     * @throws Exception
     */
    @Meter(name = "updateMessageCounter")
    @Timer(name = "updateMessageTimer")
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @RequestMapping(value = "/media/v1/images/{queryId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.PUT)
    public ResponseEntity<String> mediaUpdate(@PathVariable("queryId") final String queryId, @RequestBody final String message, @RequestHeader final MultiValueMap<String,String> headers)
            throws Exception {
        final String requestID = getRequestId(headers);
        final String clientID = getClientId();
        final String serviceUrl = MediaServiceUrl.MEDIA_IMAGES.getUrl() + "/" + queryId;
        LOGGER.info("RECEIVED UPDATE REQUEST ServiceUrl={} QueryId={} ClientId={} RequestId={} JSONMessage={}", serviceUrl, queryId, clientID, requestID,
                message);
        try {
            if (!queryId.matches(REG_EX_GUID)) {
                return buildErrorResponse("Input queryId is invalid. Must be a valid GUID in the following format [xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx]",
                        serviceUrl, BAD_REQUEST);
            }
            LOGGER.info("Started querying media by media-guid in MediaDB ClientId={} RequestId={} MediaGUID={}", clientID, requestID, queryId);
            final Media mediaInMediaDB = mediaDao.getMediaByGuid(queryId).orElseThrow(MediaNotFoundException::new);
            LOGGER.info("Finished querying media by media-guid in MediaDB ClientId={} RequestId={} MediaGUID={}", clientID, requestID, queryId);
            final String updatedJsonMessage = appendDomain(message, mediaInMediaDB.getDomainId(), mediaInMediaDB.getDomain());
            final Optional<ResponseEntity<String>> errorResponse = validateMediaUpdateRequest(mediaInMediaDB, updatedJsonMessage, serviceUrl);
            if (errorResponse.isPresent()) {
                LOGGER.warn("UPDATE VALIDATION ValidationError={} ServiceUrl={} QueryId={} ClientId={} RequestId={} JSONMessage={}",
                        errorResponse.get(), serviceUrl, queryId, clientID, requestID, message);
                return errorResponse.get();
            }
            final ImageMessage imageMessage = ImageMessage.parseJsonMessage(updatedJsonMessage);
            final ResponseEntity<String> response = mediaUpdateProcessor.processRequest(imageMessage, mediaInMediaDB);
            LOGGER.info("END UPDATE REQUEST ServiceUrl={} QueryId={} ClientId={} RequestId={} JSONMessage={}", serviceUrl, queryId, clientID, requestID, message);
            return response;
        } catch (MediaNotFoundException ex) {
            return buildErrorResponse("Requested resource with ID " + queryId + " was not found.", serviceUrl, NOT_FOUND);
        } catch (ImageMessageException ex) {
            final ResponseEntity<String> errorResponse = buildErrorResponse("JSON request format is invalid. Json message=" + message, serviceUrl, BAD_REQUEST);
            LOGGER.error(ex, "ERROR ResponseStatus={} ResponseBody={} ServiceUrl={} QueryId={} ClientId={} RequestId={} ErrorMessage={}",
                    Arrays.asList(errorResponse.getStatusCode().toString(), errorResponse.getBody(), serviceUrl, queryId, clientID,
                    requestID, ex.getMessage()), message);
            return errorResponse;
        } catch (Exception ex) {
            LOGGER.error(ex, "ERROR ServiceUrl={} QueryId={} ClientId={} RequestId={} ErrorMessage={}", Arrays.asList(serviceUrl, queryId,
                    clientID, requestID, ex.getMessage()), message);
            poker.poke("Media Services failed to process a mediaUpdate request - RequestId: " + requestID + " ClientId: " + clientID, hipChatRoom,
                    message, ex);
            throw ex;
        }
    }

    /**
     * Validates a MediaUpdate Request. Verifies is the JsonMessage is well formatted, and does not contain any malformed data.
     *
     * @param originalMedia The media record currently in the MediaDB associated with the queryId for the update request.
     * @param jsonMessage The request JsonMessage.
     * @param serviceUrl The url for MediaUpdate requests.
     * @return An Error respone ResponseEntity if the update request is not valid, otherwise null.
     * @throws Exception
     */
    private Optional<ResponseEntity<String>> validateMediaUpdateRequest(Media originalMedia, String jsonMessage, String serviceUrl) throws Exception {
        final String jsonValidationErrors = validateImageMessage(jsonMessage, "EPCUpdate");
        if (!"[]".equals(jsonValidationErrors)) {
            return Optional.of(buildErrorResponse(jsonValidationErrors, serviceUrl, BAD_REQUEST));
        }
        final ImageMessage updateMessage = ImageMessage.parseJsonMessage(jsonMessage);
        if (updateMessage.getHidden() && !canMediaBeHidden(originalMedia)) {
            return Optional.of(buildErrorResponse("Only unpublished media can be hidden", serviceUrl, BAD_REQUEST));
        }
        return Optional.empty();
    }

    /**
     * Appends Domain 'Lodging' and DomainId to the input JsonMessage.
     *
     * @param message The JsonMessage to append domainData to.
     * @param domainId The domainId to append to the input JsonMessage.
     * @param domain The domain of the original Media to append to this update request.
     * @return updated JsonMessage String.
     * @throws Exception
     */
    private String appendDomain(String message, String domainId, String domain) throws Exception {
        final Map<String, Object> jsonMap = JSONUtil.buildMapFromJson(message);
        jsonMap.put(DOMAIN, domain);
        jsonMap.put(DOMAIN_ID, domainId);
        return new ObjectMapper().writeValueAsString(jsonMap);
    }

    /**
     * Verify if a media can be hidden.
     * An image can be permanently hidden from all messages, including further updates.
     * This is not applied to published images. only unpublished images (Duplicated and Rejected) can be hidden.
     *
     * @param media Media to verify
     * @return returns true if the media can be hidden or false if not.
     */
    private Boolean canMediaBeHidden(Media media) throws Exception {
        return REJECTED_STATUS.equals(media.getStatus()) || DUPLICATED_STATUS.equals(media.getStatus());
    }

    /**
     * Web services interface to retrieve media information by its GUID.
     *
     * @param mediaGUID The GUID of the requested media.
     * @param headers Headers of the request.
     * @return The requested media information.
     * @throws Exception Thrown if processing the message fails.
     */
    @Meter(name = "getMediaByGUIDMessageCounter")
    @Timer(name = "getMediaByGUIDMessageTimer")
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @RequestMapping(value = "/media/v1/images/{mediaGUID}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.GET)
    @Transactional
    public ResponseEntity<String> getMedia(@PathVariable("mediaGUID") final String mediaGUID, @RequestHeader final MultiValueMap<String,String> headers) throws Exception {
        final String requestID = getRequestId(headers);
        final String clientID = getClientId();
        final String serviceUrl = MediaServiceUrl.MEDIA_IMAGES.getUrl() + "/" + mediaGUID;
        LOGGER.info("RECEIVED GET REQUEST ServiceUrl={} ClientId={} RequestId={} MediaGUID={}", serviceUrl, clientID, requestID, mediaGUID);
        try {
            if (!mediaGUID.matches(REG_EX_GUID)) {
                LOGGER.warn("INVALID GET REQUEST ServiceUrl={} ClientId={} RequestId={} MediaGUID={}", serviceUrl, clientID, requestID, mediaGUID);
                return buildErrorResponse("Input mediaGUID is invalid. Must be a valid GUID in the following format [xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx]", serviceUrl, BAD_REQUEST);
            }
            LOGGER.info("Started querying media by media-guid in MediaDB ClientId={} RequestId={} MediaGUID={}", clientID, requestID, mediaGUID);
            final MediaGetResponse mediaResponse = mediaGetProcessor.processMediaGetRequest(mediaGUID).orElseThrow(MediaNotFoundException::new);
            LOGGER.info("Finished querying media by media-guid in MediaDB ClientId={} RequestId={} MediaGUID={}", clientID, requestID, mediaGUID);
            return new ResponseEntity<>(OBJECT_MAPPER.writeValueAsString(mediaResponse), OK);
        } catch (MediaNotFoundException ex) {
            final ResponseEntity<String> errorResponse = buildErrorResponse("Requested resource with ID " + mediaGUID + " was not found.", serviceUrl, NOT_FOUND);
            LOGGER.info("INVALID GET REQUEST ResponseStatus={} ResponseBody={} ErrorMessage={} MediaGUID={} ClientId={} RequestId={}",
                    errorResponse.getStatusCode().toString(), errorResponse.getBody(), ex.getMessage(), mediaGUID, clientID, requestID);
            return errorResponse;
        } catch (Exception ex) {
            LOGGER.error(ex, "ERROR ServiceUrl={} ClientId={} RequestId={} MediaGuid={} ErrorMessage={}", serviceUrl, clientID, requestID, mediaGUID, ex.getMessage());
            poker.poke("Media Services failed to process a getMedia request - RequestId: " + requestID + " ClientId: " + clientID, hipChatRoom, mediaGUID, ex);
            throw ex;
        }
    }

    /**
     * Web services interface to delete media information by its GUID.
     *
     * @param mediaGUID The GUID of the media to delete.
     * @param headers Headers of the request.
     * @throws Exception Thrown if processing the message fails.
     */
    @Meter(name = "deleteMediaByGUIDMessageCounter")
    @Timer(name = "deleteMediaByGUIDMessageTimer")
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @RequestMapping(value = "/media/v1/images/{mediaGUID}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteMedia(@PathVariable("mediaGUID") final String mediaGUID, @RequestHeader final MultiValueMap<String,String> headers) throws Exception {
        final String requestID = getRequestId(headers);
        final String clientID = getClientId();
        final String serviceUrl = MediaServiceUrl.MEDIA_IMAGES.getUrl();
        try {
            LOGGER.info("RECEIVED DELETE REQUEST ServiceUrl={} ClientId={} RequestId={} MediaGUID={}", serviceUrl, clientID, requestID, mediaGUID);
            if (!mediaGUID.matches(REG_EX_GUID)) {
                LOGGER.warn("INVALID DELETE REQUEST ServiceUrl={} ClientId={} RequestId={} MediaGUID={}", serviceUrl, clientID, requestID, mediaGUID);
                return buildErrorResponse("Invalid media GUID provided.", serviceUrl, BAD_REQUEST);
            }
            final Media media = mediaDao.getMediaByGuid(mediaGUID).orElseThrow(MediaNotFoundException::new);
            media.setHidden(true);
            final ImageMessage imageMessage = media.toImageMessage();
            kafkaCommonPublisher.publishImageMessage(imageMessage, imageMessageTopic, imageMessageRetryTopic);
            return new ResponseEntity<>("Media GUID " + StringEscapeUtils.escapeHtml(mediaGUID) + " has been deleted successfully.", OK);
        } catch (MediaNotFoundException ex) {
            return buildErrorResponse("Requested resource with ID " + mediaGUID + " was not found.", serviceUrl, NOT_FOUND);
        } catch (Exception ex) {
            LOGGER.error(ex, "ERROR ServiceUrl={} ClientId={} RequestId={} MediaGuid={} ErrorMessage={}", serviceUrl, clientID, requestID, mediaGUID,
                    ex.getMessage());
            poker.poke("Media Services failed to process a deleteMedia request - RequestId: " + requestID + " ClientId: " + clientID, hipChatRoom, mediaGUID, ex);
            throw ex;
        }
    }

    /**
     * Web services interface to retrieve media information by domain name and id.
     *
     * @param domainName Name of the domain the domain id belongs to.
     * @param domainId Identification of the domain item the media is required.
     * @param activeFilter Filter determining what images to return. When true only active are returned. When false only inactive media is returned. When
     * all then all are returned. All is set a default.
     * @param derivativeTypeFilter Inclusive filter to use to only return certain types of derivatives. Returns all derivatives if not specified.
     * @param derivativeCategoryFilter Inclusive filter to use to only return certain types of medias. Returns all medias if not specified.
     * @param headers Headers of the request.
     * @param pageSize Positive integer to filter the number of media displayed per page. pageSize is inclusive with pageIndex.
     * @param pageIndex Positive integer to filter the page to display. pageIndex is inclusive with pageSize.
     * @return The list of media data belonging to the domain item.
     * @throws Exception Thrown if processing the message fails.
     */
    @Meter(name = "getMediaByDomainIdMessageCounter")
    @Timer(name = "getMediaByDomainIdMessageTimer")
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @RequestMapping(value = "/media/v1/imagesbydomain/{domainName}/domainId/{domainId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.GET)
    @Transactional
    public ResponseEntity<String> getMediaByDomainId(@PathVariable("domainName") final String domainName, @PathVariable("domainId") final String domainId,
            @RequestParam(value = "pageSize", required = false) final Integer pageSize,
            @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
            @RequestParam(value = "activeFilter", required = false, defaultValue = "all") final String activeFilter,
            @RequestParam(value = "derivativeTypeFilter", required = false) final String derivativeTypeFilter,
            @RequestParam(value = "derivativeCategoryFilter", required = false) final String derivativeCategoryFilter,
            @RequestHeader final MultiValueMap<String,String> headers) throws Exception {
        final String requestID = getRequestId(headers);
        final String clientID = getClientId();
        final String serviceUrl = MediaServiceUrl.MEDIA_BY_DOMAIN.getUrl();
        LOGGER.info("RECEIVED GET BY DOMAIN ID REQUEST ServiceUrl={} ClientId={} RequestId={} DomainName={} DomainId={} PageSize={} PageIndex={} ActiveFilter={} " +
                        "DerivativeTypeFilter={}",
                serviceUrl, clientID, requestID, domainName, domainId, pageSize, pageIndex, activeFilter, derivativeTypeFilter);
        try {
            final ResponseEntity<String> errorResponse = validateMediaByDomainIdRequest(domainName, domainId, activeFilter, pageSize, pageIndex);
            if (errorResponse != null) {
                LOGGER.warn("INVALID GET BY DOMAIN ID REQUEST ResponseStatus={} ResponseBody={} ServiceUrl={} ClientId={} RequestId={} DomainName={} DomainId={} PageSize={} " +
                                "PageIndex={} ActiveFilter={} DerivativeTypeFilter={}",
                        errorResponse.getStatusCode().toString(), errorResponse.getBody(), serviceUrl, clientID, requestID, domainName, domainId, pageSize, pageIndex,
                        activeFilter, derivativeTypeFilter);
                return errorResponse;
            }
            LOGGER.info("Started querying media by domainId in MediaDB ClientId={} RequestId={} DomainName={} DomainId={}", clientID, requestID, domainName, domainId);
            final MediaByDomainIdResponse response = mediaGetProcessor.processMediaByDomainIDRequest(Domain.findDomain(domainName, true), domainId, activeFilter, derivativeTypeFilter,
                    derivativeCategoryFilter, pageSize, pageIndex);
            LOGGER.info("Finished querying media by domainId in MediaDB ClientId={} RequestId={} DomainName={} DomainId={}", clientID, requestID, domainName, domainId);
            return new ResponseEntity<>(OBJECT_MAPPER.writeValueAsString(response), OK);
        } catch (PaginationValidationException p) {
            return buildErrorResponse(p.getMessage(), serviceUrl, BAD_REQUEST);
        } catch (Exception ex) {
            LOGGER.warn(ex, "INVALID GET BY DOMAIN ID REQUEST ServiceUrl={} ClientId={} RequestId={} DomainName={} DomainId={} PageSize={} PageIndex={} ActiveFilter={} " +
                            "DerivativeTypeFilter={}",
                    serviceUrl, clientID, requestID, domainName, domainId, pageSize, pageIndex, activeFilter, derivativeTypeFilter);
            poker.poke("Media Services failed to process a getMediaByDomainId request - RequestId: " + requestID + " ClientId: " + clientID, hipChatRoom,
                    domainId, ex);
            throw ex;
        }
    }

    /**
     * Validates the media by domain id request.
     *
     * @param domainName Domain to validate.
     * @param domainId DomainId to validate if it exists.
     * @param activeFilter Active filter to validate.
     * @param pageSize The pagSize value to validate.
     * @param pageIndex The pageIndex value to validate.
     * @return Returns a response if the validation fails; null otherwise.
     */
    private ResponseEntity<String> validateMediaByDomainIdRequest(final String domainName, String domainId, final String activeFilter, Integer pageSize, Integer pageIndex) {
        if (activeFilter != null && !activeFilter.equalsIgnoreCase("all") && !activeFilter.equalsIgnoreCase("true")
                && !activeFilter.equalsIgnoreCase("false")) {
            return new ResponseEntity<>("Unsupported active filter " + activeFilter, BAD_REQUEST);
        }
        final Domain domain = Domain.findDomain(domainName, true);
        if (domain == null) {
            return new ResponseEntity<>("Domain not found " + domainName, NOT_FOUND);
        }
        if (Domain.LODGING.getDomain().equalsIgnoreCase(domain.getDomain()) && !lodgingReferenceHotelIdDao.domainIdExists(domainId)) {
            return new ResponseEntity<>("DomainId not found: " + domainId, NOT_FOUND);
        }
        if (pageSize != null || pageIndex != null) {
            if (pageIndex == null) {
                throw new PaginationValidationException("pageIndex is null and pageSize is not null, both pageSize and pageIndex parameters are inclusive. " +
                        "Set both parameters or neither.");
            } else if (pageSize == null) {
                throw new PaginationValidationException("pageSize is null and pageIndex is not null, both pageSize and pageIndex parameters are inclusive. " +
                        "Set both parameters or neither.");
            } else if (pageSize < 0 || pageIndex < 0) {
                throw new PaginationValidationException("pageSize and pageIndex do not accept negative values.");
            }
        }
        return null;
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

    /**
     * Get validator list by different client, and do validation by rules and DAO validator (later)
     * return the validation error list that combine all of the error result.
     *
     * @param message input json message.
     * @param clientId Web service client id.
     * @return JSON string contains fileName and error description.
     * @throws Exception when message to ImageMessage and convert java list to json.
     */
    private String validateImageMessage(final String message, final String clientId) {
        final List<MapMessageValidator> defaultValidatorList = mapValidatorList.get(DEFAULT_VALIDATION_RULES);
        final List<MapMessageValidator> validatorList = mapValidatorList.getOrDefault(clientId, defaultValidatorList);
        if (validatorList == null && defaultValidatorList == null) {
            return UNAUTHORIZED_USER_MESSAGE;
        }
        if (validatorList == defaultValidatorList) {
            LOGGER.warn("NO VALIDATION FOR CLIENT Action=\"Using default validations\" ClientId={}", Arrays.asList(clientId), message);
        }
        List<String> validationErrorList = null;
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
        for (final MapMessageValidator mapMessageValidator : validatorList) {
            validationErrorList = mapMessageValidator.validateImages(Arrays.asList(imageMessage));
            if (!validationErrorList.isEmpty()) {
                return JSONUtil.convertValidationErrors(validationErrorList);
            }
        }
        return JSONUtil.convertValidationErrors(validationErrorList);
    }

    /**
     * runs every hour to reprocess media log entry in queue.
     * @throws IOException
     */
    @Scheduled(fixedRate = ONE_HOUR)
    public void reprocessMediaLog() throws IOException {
        final Message<?> message = messagingTemplate.receive(mediaProcessLogQueue);
        if (message != null) {
            final String json = (String) message.getPayload();
            LOGGER.info("reprocess media log from queue JsonMessage={}", json);
            final LogEntry logEntry = LogEntry.getLogFromMessage(json);
            logActivityProcess.log(logEntry, reporting);
        }
    }
}
