package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;
import com.expedia.content.media.processing.services.dao.DomainNotFoundException;
import com.expedia.content.media.processing.services.util.*;
import com.expedia.content.media.processing.services.validator.S3Validator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import expedia.content.solutions.metrics.annotations.Counter;
import expedia.content.solutions.metrics.annotations.Meter;
import expedia.content.solutions.metrics.annotations.Timer;
import expedia.content.solutions.metrics.spring.EnableMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ResourceBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * MPP media service application.
 * This class has the main Spring configuration and also the bootstrap for the application.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.expedia.content.media.processing")
@ImportResource("classpath:media-services.xml")
@RestController
@EnableMetrics
public class Application extends SpringBootServletInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final String REQUESTID = "request-id";
    private static final int BAD_REQUEST_CODE = 400;
    private static final int UNAUTHOIRZED_CODE = 401;
    private static final int NOT_FOUND_CODE = 404;
    private static final int SERVER_ERROR_CODE = 500;
    private static final Map<Integer, String> ERROR_CODE_MAP;

    static {
        ERROR_CODE_MAP = new HashMap<>();
        ERROR_CODE_MAP.put(BAD_REQUEST_CODE, "Bad Request");
        ERROR_CODE_MAP.put(UNAUTHOIRZED_CODE, "Unauthorized");
        ERROR_CODE_MAP.put(NOT_FOUND_CODE, "Not Found");
        ERROR_CODE_MAP.put(SERVER_ERROR_CODE, " Server Error");
    }

    private static final Map<Integer, HttpStatus> HTTP_STATUS_MAP;

    static {
        HTTP_STATUS_MAP = new HashMap<>();
        HTTP_STATUS_MAP.put(BAD_REQUEST_CODE, HttpStatus.BAD_REQUEST);
        HTTP_STATUS_MAP.put(UNAUTHOIRZED_CODE, HttpStatus.UNAUTHORIZED);
        HTTP_STATUS_MAP.put(NOT_FOUND_CODE, HttpStatus.NOT_FOUND);
        HTTP_STATUS_MAP.put(SERVER_ERROR_CODE, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Autowired
    private MediaServiceProcess mediaServiceProcess;
    @Resource(name = "providerProperties")
    private Properties providerProperties;

    @Autowired
    private RouterUtil routerUtil;

    @Autowired
    private RestClient mediaServiceClient;

    public static void main(String[] args) throws Exception {
        final SpringApplication application = new SpringApplicationBuilder()
                .banner(new ResourceBanner(new DefaultResourceLoader().getResource("banner.txt")))
                .child(Application.class)
                .build();
        application.run(args);
    }

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
    @RequestMapping(value = "/acquireMedia", method = RequestMethod.POST)
    public ResponseEntity<String> acquireMedia(@RequestBody final String message, @RequestHeader MultiValueMap<String, String> headers) throws Exception {
        return acquireMedia(message, MediaServiceUrl.ACQUIRE_MEDIA, headers);
    }

    /**
     * Web service interface to push a media file into the media processing pipeline.
     * Validation will be done before message go through.
     *
     * @param message
     * @param serviceUrl
     * @return ResponseEntity Standard spring response object.
     * @throws Exception Thrown if processing the message fails.
     */
    private ResponseEntity<String> acquireMedia(final String message, final MediaServiceUrl serviceUrl, @RequestHeader MultiValueMap<String, String> headers)
            throws Exception {
        LOGGER.info("RECEIVED REQUEST - message=" + serviceUrl.getUrl().toString() + ", message=[{}], requestId=[{}]", message, headers.get(REQUESTID));
        try {
            ImageMessage imageMessageOld = ImageMessage.parseJsonMessage(message);
            Map messageMap = JSONUtil.buildMapFromJson(message);
            JSONUtil.addGuidToMap(messageMap);
            String mediaCommonMessage = JSONUtil.convertToCommonMessage(imageMessageOld, messageMap, providerProperties);
            LOGGER.info("converted to - common message =[{}]", mediaCommonMessage);
            ImageMessage imageMessageCommon = ImageMessage.parseJsonMessage(mediaCommonMessage);
            boolean sendToAWS = routerUtil.routeAWSByPercentage();
            LOGGER.debug("send message to AWS {sendToAWS}", sendToAWS);
            //new mediaCommon Message.
            if (sendToAWS) {
                //reuse current validation logic
                String userName = "EPC";
                String json = mediaServiceProcess.validateImageMessage(mediaCommonMessage, userName);
                if (!"[]".equals(json)) {
                    return buildBadRequestResponse(json, serviceUrl.getUrl().toString());
                }
                mediaServiceProcess.publishMsg(imageMessageCommon, mediaCommonMessage);
                LOGGER.info("SUCCESS - send message to AWS media service  - message=[{}], requestId=[{}]", mediaCommonMessage,
                        headers.get(REQUESTID));
                return new ResponseEntity<>("OK,message sent to AWS queue successfully.", HttpStatus.OK);
            } else {
                String response = mediaServiceClient.callMediaService(message);
                LOGGER.info("SUCCESS send message to media service  - message=[{}], response=[{}]", message, response);
                return new ResponseEntity<>("OK,message sent to mpp media service successfully.", HttpStatus.OK);
            }

        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - messageName={}, JSONMessage=[{}], requestId=[{}] .", serviceUrl.getUrl().toString(), message, ex, headers.get(REQUESTID));
            return buildBadRequestResponse("JSON request format is invalid. Json message=" + message, serviceUrl.getUrl().toString());
        }
    }

    /**
     * Web service interface to push a media file into the media processing pipeline.
     *
     * @param message JSON formated ImageMessage. fileUrl and expediaId are required.
     * @return ResponseEntity Standard spring response object.
     * @throws Exception Thrown if processing the message fails.
     * @see com.expedia.content.media.processing.pipeline.domain.ImageMessage
     */
    @Meter(name = "addMessageCounter")
    @Timer(name = "addMessageTimer")
    @RequestMapping(value = "/media/v1/images", method = RequestMethod.POST)
    public ResponseEntity<String> mediaAdd(@RequestBody final String message, @RequestHeader MultiValueMap<String, String> headers) throws Exception {
        return mediaAdd(message, MediaServiceUrl.MEDIA_ADD, headers);
    }

    /**
     * Web service interface to push a media file into the media processing pipeline.
     * Validation will be done before message go through.
     *
     * @param message
     * @param serviceUrl
     * @return ResponseEntity Standard spring response object.
     * @throws Exception Thrown if processing the message fails.
     */
    private ResponseEntity<String> mediaAdd(final String message, final MediaServiceUrl serviceUrl, @RequestHeader MultiValueMap<String, String> headers)
            throws Exception {
        LOGGER.info("RECEIVED REQUEST - message=" + serviceUrl.getUrl().toString() + ", message=[{}], requestId=[{}]", message, headers.get(REQUESTID));
        try {
            ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userName = auth.getName();
            String json = mediaServiceProcess.validateImageMessage(message, userName);
            if (!"[]".equals(json)) {
                return buildBadRequestResponse(json, serviceUrl.getUrl().toString());
            }
            //TODO Fix this to not throw a bad request if the URL does not start with the S3 protocol or throw bad request when 404 on HTTP
            boolean fileExists = S3Validator.checkFileExists(imageMessage.getFileUrl());
            if (!fileExists) {
                return buildBadRequestResponse("fileUrl does not exist in s3.", serviceUrl.getUrl().toString());
            }
            final String guid = UUID.randomUUID().toString();
            ImageMessage.ImageMessageBuilder imageMessageBuilder = new ImageMessage.ImageMessageBuilder();

            imageMessageBuilder = imageMessageBuilder.transferAll(imageMessage);
            if (imageMessage.getCaption() == null) {
                imageMessageBuilder.caption("");
            }
            if (imageMessage.getMediaGuid() == null) {
                imageMessageBuilder.mediaGuid(guid);
            }
            ImageMessage imageMessageNew = imageMessageBuilder.clientId(userName).requestId(String.valueOf(headers.get(REQUESTID))).build();
            /* TODO Change fileName in image message to be built with a hash of the url except when the client is multisource. 
             * Multisource will have to use the eid and provider id still until the filename keys are replaced in the fingerprint table.
             */
            mediaServiceProcess.publishMsg(imageMessageNew);
            LOGGER.info("SUCCESS - messageName={}, JSONMessage=[{}], requestId=[{}]", serviceUrl.getUrl().toString(), message, headers.get(REQUESTID));
            return new ResponseEntity<>("OK", HttpStatus.OK);
        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - messageName={}, JSONMessage=[{}], requestId=[{}].", serviceUrl.getUrl().toString(), message, ex, headers.get(REQUESTID));
            return buildBadRequestResponse("JSON request format is invalid. Json message=" + message, serviceUrl.getUrl().toString());
        }
    }

    /**
     * Web service interface to get the latest media file process status.
     *
     * @param message JSON formatted message, "mediaNames", contains an array of media file names.
     * @return ResponseEntity Standard spring response object.
     * @throws Exception Thrown if processing the message fails.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Meter(name = "mediaLatestStatusCounter")
    @Timer(name = "mediaLatestStatusTimer")
    @RequestMapping(value = "/media/v1/lateststatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getMediaLatestStatus(@RequestBody final String message, @RequestHeader MultiValueMap<String, String> headers) throws Exception {
        LOGGER.info("RECEIVED REQUEST - url= [{}]" + MediaServiceUrl.MEDIA_STATUS.getUrl().toString() + ", imageMessage=[{}], requestId=[{}]", message,
                headers.get(REQUESTID));
        try {
            Map<String, Object> map = JSONUtil.buildMapFromJson(message);
            ValidationStatus validationStatus = mediaServiceProcess.validateMediaStatus(message);
            if (!validationStatus.isValid()) {
                return buildBadRequestResponse(validationStatus.getMessage(), MediaServiceUrl.MEDIA_STATUS.getUrl().toString());
            }
            String jsonResponse = mediaServiceProcess.getMediaStatusList((List<String>) map.get("mediaNames"));
            LOGGER.info("RESPONSE - url=[{}]", MediaServiceUrl.MEDIA_STATUS.getUrl(), toString() + ", imageMessage=[{}], requestId=[{}]", jsonResponse,
                    headers.get(REQUESTID));
            return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        } catch (RequestMessageException ex) {
            LOGGER.error("ERROR - url=[{}], imageMessage=[{}], error=[{}], requestId=[{}]",
                    MediaServiceUrl.MEDIA_STATUS.getUrl(), message, ex.getMessage(), ex, headers.get(REQUESTID));
            return buildBadRequestResponse(ex.getMessage(), MediaServiceUrl.MEDIA_STATUS.getUrl().toString());

        }
    }

    /**
     * Builds a Bad Request response for when the incoming message fails validation.
     * Note that the {@code @Meter} {@code @Timer} annotations introduce aspects from metrics-support
     *
     * @param validationMessage, failed message from validate.
     * @return A Bad Request response.
     */
    @Counter(name = "badRequestCounter")
    public ResponseEntity<String> buildBadRequestResponse(String validationMessage, String url) {
        String resMsg = JSONUtil.generateJsonForErrorResponse(validationMessage, url, BAD_REQUEST_CODE, "Bad Request");
        return new ResponseEntity<>(resMsg, HttpStatus.BAD_REQUEST);
    }

    /**
     * Builds a Bad Request response for when the incoming message fails validation.
     * Note that the {@code @Meter} {@code @Timer} annotations introduce aspects from metrics-support
     *
     * @param validationMessage, failed message from validate.
     * @param errorCode          error code thrown
     * @return A Bad Request response.
     */
    @Counter(name = "badRequestCounter")
    public ResponseEntity<String> buildBadRequestResponse(String validationMessage, String url, int errorCode) {
        String resMsg = JSONUtil.generateJsonForErrorResponse(validationMessage, url, errorCode, ERROR_CODE_MAP.get(errorCode));
        return new ResponseEntity<>(resMsg, HTTP_STATUS_MAP.get(errorCode));
    }


    /**
     * listen for message from media service queue and publish to collector queue again.
     * No validation happen here.
     *
     * @param message
     */
    @MessageMapping("${media.aws.service.queue.name}")
    public void pollMessage(String message) {
        LOGGER.info("Receiving msg: {}", message);
        try {
            ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
            mediaServiceProcess.publishMsg(imageMessage);
        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - messageName={}, JSONMessage=[{}] .", message, ex);
        }
    }

    /**
     * Image response for Media domain categories service.
     *
     * @param headers    Header header contains the requestId and the clientId.
     * @param domainName associated domain.
     * @param localeId   Localization.
     * @return returns a JSON response for domain categories request.
     * @throws Exception
     */
    @RequestMapping(value = "/media/v1/domaincategories/{domainName}", method = RequestMethod.GET)
    public ResponseEntity<String> domainCategories(
            @RequestHeader MultiValueMap<String, String> headers,
            @PathVariable("domainName") String domainName,
            @RequestParam(value = "localeId", required = false) String localeId) throws Exception {
        String localePath = (localeId == null) ? "" : "?localeId=" + localeId;
        LOGGER.info("RECEIVED REQUEST - url=[{}][{}][{}], requestId=[{}]",
                MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl(), domainName, localePath, headers.get(REQUESTID));
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userName = auth.getName();
            String json = mediaServiceProcess.validateDomainCategoriesRequest(userName);
            if (!"OK".equals(json)) {
                return buildBadRequestResponse(json, MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl());
            }
            final String response = mediaServiceProcess.getDomainCategories(domainName, localeId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (DomainNotFoundException e) {
            LOGGER.error("ERROR - JSONMessage=[{}], requestId=[{}]", e, headers.get(REQUESTID));
            return buildBadRequestResponse("Requested resource with ID " + domainName + " was not found.",
                    MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl() + domainName + localePath, NOT_FOUND_CODE);
        } catch (Exception e) {
            LOGGER.error("ERROR - JSONMessage=[{}], requestId=[{}]", e, headers.get(REQUESTID));
            return buildBadRequestResponse(e.getMessage(), MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl() + domainName + localePath);
        }
    }
}
