package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;
import com.expedia.content.media.processing.services.dao.DomainNotFoundException;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import com.expedia.content.media.processing.services.util.RequestMessageException;
import com.expedia.content.media.processing.services.util.RestClient;
import com.expedia.content.media.processing.services.util.RouterUtil;
import com.expedia.content.media.processing.services.validator.S3Validator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import expedia.content.solutions.metrics.annotations.Counter;
import expedia.content.solutions.metrics.annotations.Meter;
import expedia.content.solutions.metrics.annotations.Timer;
import expedia.content.solutions.metrics.spring.EnableMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * MPP media service application.
 * This class has the main Spring configuration and also the bootstrap for the application.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.expedia.content.media.processing")
@ImportResource("classpath:media-services.xml")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@RestController
@EnableMetrics
public class Application extends SpringBootServletInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final String REQUESTID = "request-id";
    private static final int BAD_REQUEST_CODE = 400;
    private static final int NOT_FOUND_CODE = 404;
    private static final Map<Integer, HttpStatus> HTTP_STATUS_MAP;

    static {
        HTTP_STATUS_MAP = new HashMap<>();
        HTTP_STATUS_MAP.put(BAD_REQUEST_CODE, HttpStatus.BAD_REQUEST);
        HTTP_STATUS_MAP.put(NOT_FOUND_CODE, HttpStatus.NOT_FOUND);
    }


    @Autowired
    private MediaServiceProcess mediaServiceProcess;
    @Autowired
    private RouterUtil routerUtil;
    @Autowired
    private RestClient mediaServiceClient;
    @Resource(name = "providerProperties")
    private Properties providerProperties;

    @Value("${media.router.providers}")
    private String providerRouters;


    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
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
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
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
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private ResponseEntity<String> acquireMedia(final String message, final MediaServiceUrl serviceUrl, @RequestHeader MultiValueMap<String, String> headers)
            throws Exception {
        final List<String> requestID = headers.get(REQUESTID);
        LOGGER.info("RECEIVED REQUEST - messageName={}, JSONMessage=[{}], requestId=[{}]", serviceUrl.getUrl(), message, requestID);
        try {
            final ImageMessage imageMessageOld = ImageMessage.parseJsonMessage(message);
            final Map messageMap = JSONUtil.buildMapFromJson(message);
            JSONUtil.addGuidToMap(messageMap);
            final String mediaCommonMessage = JSONUtil.convertToCommonMessage(imageMessageOld, messageMap, providerProperties);
            LOGGER.info("converted to - common message =[{}]", mediaCommonMessage);
            final ImageMessage imageMessageCommon = ImageMessage.parseJsonMessage(mediaCommonMessage);
            final String routeName = getRouteNameByProvider(imageMessageCommon);
            final boolean sendToAWS = routerUtil.routeAWSByPercentage(routeName);
            LOGGER.debug("send message to AWS {}", sendToAWS);
            //new mediaCommon Message.
            if (sendToAWS) {
                //reuse current validation logic
                final String userName = "EPC";
                final String json = mediaServiceProcess.validateImageMessage(mediaCommonMessage, userName);
                if (!"[]".equals(json)) {
                    return buildErrorCodeResponse(json, serviceUrl.getUrl(), BAD_REQUEST_CODE);
                }
                mediaServiceProcess.publishMsg(imageMessageCommon, mediaCommonMessage);
                LOGGER.info("SUCCESS - send message to AWS media service  - common message=[{}], requestId=[{}]", mediaCommonMessage,
                        requestID);
                return new ResponseEntity<>("OK,message sent to AWS queue successfully.", HttpStatus.OK);
            } else {
                final String response = mediaServiceClient.callMediaService(message);
                LOGGER.info("SUCCESS send message to media service  - JSONMessage=[{}], response=[{}], requestId=[{}]", message, response, requestID);
                return new ResponseEntity<>("OK,message sent to mpp media service successfully.", HttpStatus.OK);
            }

        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - messageName={}, JSONMessage=[{}], error=[{}], requestID=[{}] .", serviceUrl.getUrl(), message, ex, requestID);
            return buildErrorCodeResponse("JSON request format is invalid. Json message=" + message, serviceUrl.getUrl(), BAD_REQUEST_CODE);
        }
    }

    private String getRouteNameByProvider(ImageMessage imageMessage) {
        return (imageMessage.getOuterDomainData() != null && providerRouters.contains(imageMessage.getOuterDomainData().getProvider())) ?
                imageMessage.getOuterDomainData().getProvider() : RouterUtil.DEFAULT_ROUTER_NAME;
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
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
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
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private ResponseEntity<String> mediaAdd(final String message, final MediaServiceUrl serviceUrl, @RequestHeader MultiValueMap<String, String> headers)
            throws Exception {
        final List<String> requestID = headers.get(REQUESTID);
        LOGGER.info("RECEIVED REQUEST - messageName={} , JSONMessage=[{}], requestId=[{}]", serviceUrl.getUrl(), message, requestID);
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final String userName = auth.getName();
            final String json = mediaServiceProcess.validateImageMessage(message, userName);
            if (!"[]".equals(json)) {
                return buildErrorCodeResponse(json, serviceUrl.getUrl(), BAD_REQUEST_CODE);
            }
            //TODO Fix this to not throw a bad request if the URL does not start with the S3 protocol or throw bad request when 404 on HTTP
            final ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
            final boolean fileExists = S3Validator.checkFileExists(imageMessage.getFileUrl());
            if (!fileExists) {
                LOGGER.info("Response bad request 'fileUrl does not exist in s3' for -message=[{}], requestId=[{}]", message, requestID);
                return buildErrorCodeResponse("fileUrl does not exist in s3.", serviceUrl.getUrl(), BAD_REQUEST_CODE);
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
            final ImageMessage imageMessageNew = imageMessageBuilder.clientId(userName).requestId(String.valueOf(requestID)).build();
            /* TODO Change fileName in image message to be built with a hash of the url except when the client is multisource. 
             * Multisource will have to use the eid and provider id still until the filename keys are replaced in the fingerprint table.
             */
            mediaServiceProcess.publishMsg(imageMessageNew);
            LOGGER.info("SUCCESS - messageName={}, JSONMessage=[{}], requestId=[{}]", serviceUrl.getUrl(), message, requestID);
            return new ResponseEntity<>("OK", HttpStatus.OK);
        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - messageName={}, JSONMessage=[{}], error=[{}], requestId=[{}] .", serviceUrl.getUrl(), message, ex, requestID);
            return buildErrorCodeResponse("JSON request format is invalid. Json message=" + message, serviceUrl.getUrl(), BAD_REQUEST_CODE);
        }
    }

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
    @RequestMapping(value = "/media/v1/lateststatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getMediaLatestStatus(@RequestBody final String message, @RequestHeader MultiValueMap<String, String> headers) throws Exception {
        final List<String> requestID = headers.get(REQUESTID);
        LOGGER.info("RECEIVED REQUEST - url=[{}], imageMessage=[{}], requestId=[{}]", MediaServiceUrl.MEDIA_STATUS.getUrl(), message,
                requestID);
        try {
            final ValidationStatus validationStatus = mediaServiceProcess.validateMediaStatus(message);
            if (!validationStatus.isValid()) {
                return buildErrorCodeResponse(validationStatus.getMessage(), MediaServiceUrl.MEDIA_STATUS.getUrl(), BAD_REQUEST_CODE);
            }
            final Map<String, Object> map = JSONUtil.buildMapFromJson(message);
            final String jsonResponse = mediaServiceProcess.getMediaStatusList((List<String>) map.get("mediaNames"));
            LOGGER.info("RESPONSE - url=[{}], imageMessage=[{}], requestId=[{}]", MediaServiceUrl.MEDIA_STATUS.getUrl(), jsonResponse,
                    requestID);
            return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        } catch (RequestMessageException ex) {
            LOGGER.error("ERROR - url=[{}], imageMessage=[{}], error=[{}], requestId=[{}]", MediaServiceUrl.MEDIA_STATUS.getUrl(), message,
                    ex.getMessage(), ex, requestID);
            return buildErrorCodeResponse(ex.getMessage(), MediaServiceUrl.MEDIA_STATUS.getUrl(), BAD_REQUEST_CODE);
        }
    }

    /**
     * Image response for Media domain categories service.
     *
     * @param headers    Header header contains the requestId and the clientId.
     * @param domainName associated domain.
     * @param localeId   Localization.
     * @return returns a JSON response for domain categories request.
     */
    @RequestMapping(value = "/media/v1/domaincategories/{domainName}", method = RequestMethod.GET)
    public ResponseEntity<String> domainCategories(
            @RequestHeader MultiValueMap<String, String> headers,
            @PathVariable("domainName") String domainName,
            @RequestParam(value = "localeId", required = false) String localeId) {
        final String localePath = (localeId == null) ? "" : "?localeId=" + localeId;
        LOGGER.info("RECEIVED REQUEST - url=[{}][{}][{}], requestId=[{}]",
                MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl(), domainName, localePath, headers.get(REQUESTID));
        try {
            final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            final String userName = auth.getName();
            final String json = mediaServiceProcess.validateDomainCategoriesRequest(userName);
            if (!"OK".equals(json)) {
                return buildErrorCodeResponse(json, MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl(), BAD_REQUEST_CODE);
            }
            final String response = mediaServiceProcess.getDomainCategories(domainName, localeId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (DomainNotFoundException e) {
            LOGGER.error("ERROR - JSONMessage=[{}], requestId=[{}]", e, headers.get(REQUESTID));
            return buildErrorCodeResponse("Requested resource with ID " + domainName + " was not found.",
                    MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl() + domainName + localePath, NOT_FOUND_CODE);
        }
    }

    /**
     * Builds a response for when the incoming message fails validation.
     * Note that the {@code @Meter} {@code @Timer} annotations introduce aspects from metrics-support
     *
     * @param validationMessage     failed message from validate.
     * @param errorCode             error code thrown
     * @return A error status response.
     */
    @Counter(name = "badRequestCounter")
    private ResponseEntity<String> buildErrorCodeResponse(String validationMessage, String url, int errorCode) {
        final String resMsg = JSONUtil.generateJsonForErrorResponse(validationMessage, url, errorCode, HTTP_STATUS_MAP.get(errorCode).getReasonPhrase());
        return new ResponseEntity<>(resMsg, HTTP_STATUS_MAP.get(errorCode));
    }
}
