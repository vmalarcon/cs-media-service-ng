package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
    private static final int POLL_MSG_INTERVAL = 360000;


    @Autowired
    private MediaServiceProcess mediaServiceProcess;
    @Resource(name = "providerProperties")
    private Properties providerProperties;

    @Autowired
    RouterUtil routerUtil;

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
    private ResponseEntity<String> acquireMedia(final String message, final MediaServiceUrl serviceUrl,
            @RequestHeader MultiValueMap<String, String> headers)
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
            //reuse current validation logic
            String userName = "EPC";
            String json = mediaServiceProcess.validateImageMessage(mediaCommonMessage, userName);
            if (!"[]".equals(json)) {
                return buildBadRequestResponse(json, serviceUrl.getUrl().toString());
            }
            //new mediaCommon Message.
            if (sendToAWS) {
                mediaServiceProcess.publishMsg(imageMessageCommon, mediaCommonMessage, false);
            } else {
                mediaServiceProcess.publishMsg(imageMessageCommon, message, true);
            }
            LOGGER.info("SUCCESS - messageName={}, JSONMessage=[{}], requestId=[{}]", serviceUrl.getUrl().toString(), message, headers.get(REQUESTID));
            return new ResponseEntity<>("OK", HttpStatus.OK);
        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - messageName={}, JSONMessage=[{}] .", serviceUrl.getUrl().toString(), message, ex);
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
            boolean fileExists = S3Validator.checkFileExists(imageMessage.getFileUrl());
            if (fileExists == false) {
                return buildBadRequestResponse("fileUrl does not exist in s3.", serviceUrl.getUrl().toString());
            }
            mediaServiceProcess.publishMsg(imageMessage);
            LOGGER.info("SUCCESS - messageName={}, JSONMessage=[{}], requestId=[{}]", serviceUrl.getUrl().toString(), message, headers.get(REQUESTID));
            return new ResponseEntity<>("OK", HttpStatus.OK);
        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - messageName={}, JSONMessage=[{}] .", serviceUrl.getUrl().toString(), message, ex);
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
            LOGGER.error("ERROR - url=[{}], imageMessage=[{}], error=[{}]", MediaServiceUrl.MEDIA_STATUS.getUrl(), message, ex.getMessage(), ex);
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
     * test rabbit mq performance
     *
     * @return
     */
    @RequestMapping(value = "/media/rabbitmq", method = RequestMethod.GET)
    public ResponseEntity putRabbitMq(@RequestParam(value = "testRabbit", required = false) Boolean testRabbit) {
        long timeUsed = mediaServiceProcess.publishRabbitMsg(testRabbit);
        return new ResponseEntity<>("put msg to rabbitmq used time:" + timeUsed, HttpStatus.OK);
    }

}
