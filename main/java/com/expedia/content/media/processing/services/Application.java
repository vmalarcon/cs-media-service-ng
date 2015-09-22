package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import com.expedia.content.media.processing.services.util.RequestMessageException;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import com.expedia.content.metrics.aspects.EnableMonitoringAspects;
import com.expedia.content.metrics.aspects.annotations.Counter;
import com.expedia.content.metrics.aspects.annotations.Meter;
import com.expedia.content.metrics.aspects.annotations.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.List;
import java.util.Map;

/**
 * MPP media service application.
 * This class has the main Spring configuration and also the bootstrap for the application.
 */
@Configuration
@ComponentScan(basePackages = "com.expedia.content.media.processing")
@ImportResource("classpath:media-services.xml")
@RestController
@EnableAutoConfiguration
@EnableMonitoringAspects
public class Application {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final String REQUESTID = "request-id";
    private static final int BAD_REQUEST_CODE = 400;

    @Autowired
    private MediaServiceProcess mediaServiceProcess;
    
    public static void main(final String[] args) {
        new SpringApplicationBuilder().showBanner(true).sources(Application.class).run(args);
    }
    
    /**
     * web service interface to consume media message.
     * Note that the {@code @Meter} {@code @Timer} {@code @Retryable} annotations introduce aspects from metrics-support and spring-retry
     * modules. The aspects should be applied in order, Metrics being outside (outer) and retry being inside (inner).
     *
     * @param message is json format media message,fileUrl and expedia is required.
     * @return ResponseEntity Standard spring response object.
     * @throws Exception Thrown if processing the message fails.
     * @deprecated Replaced by {@code mediaAdd(String message)} method.
     */
    @Meter(name = "acquireMessageCounter")
    @Timer(name = "acquireMessageTimer")
    @RequestMapping(value = "/acquireMedia", method = RequestMethod.POST)
    @Deprecated
    public ResponseEntity<String> acquireMedia(@RequestBody final String message) throws Exception {
        return mediaAdd(message);
    }
    
    /**
     * Web service interface to push a media file into the media processing pipeline. 
     *
     * @param message JSON formated ImageMessage. fileUrl and expediaId are required.
     * @return ResponseEntity Standard spring response object.
     * @throws Exception Thrown if processing the message fails.
     * @see com.expedia.content.media.processing.domain.ImageMessage
     */
    @Meter(name = "addMessageCounter")
    @Timer(name = "addMessageTimer")
    @RequestMapping(value = "/media/v1/add", method = RequestMethod.POST)
    public ResponseEntity<String> mediaAdd(@RequestBody final String message) throws Exception {
        LOGGER.info("RECEIVED REQUEST - message=/media/v1/add, image_message=[{}]", message);
        try {
            ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
            ValidationStatus validationStatus = mediaServiceProcess.validateImage(imageMessage);
            if (!validationStatus.isValid()) {
                return buildBadRequestResponse(validationStatus.getMessage(), MediaServiceUrl.ACQUIREMEDIA.getUrl().toString());
            }
            mediaServiceProcess.publishMsg(imageMessage);
            LOGGER.info("SUCCESS - message=/media/v1/add, image_message=[{}]", message);
            return new ResponseEntity<>("OK", HttpStatus.OK);
        } catch (IllegalStateException | ImageMessageException ex) {
            LOGGER.error("ERROR - message=/media/v1/add, image_message=[{}] .", message, ex);
            return buildBadRequestResponse("JSON request format is invalid. Json message=" + message, MediaServiceUrl.ACQUIREMEDIA.getUrl().toString());
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
        LOGGER.info("RECEIVED REQUEST - url=" + MediaServiceUrl.MEDIASTATUS.getUrl().toString() + ", image_message=[{}], request-id=[{}]", message,
                headers.get(REQUESTID));
        try {
            Map<String, Object> map = JSONUtil.buildMapFromJson(message);
            ValidationStatus validationStatus = mediaServiceProcess.validateMediaStatus(message);
            if (!validationStatus.isValid()) {
                return buildBadRequestResponse(validationStatus.getMessage(), MediaServiceUrl.MEDIASTATUS.getUrl().toString());
            }
            String jsonResponse = mediaServiceProcess.getMediaStatusList((List<String>) map.get("mediaNames"));
            LOGGER.info("RESPONSE - url=" + MediaServiceUrl.MEDIASTATUS.getUrl(), toString() + ", image_message=[{}] request-id=[{}]", jsonResponse,
                    headers.get(REQUESTID));
            return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        } catch (RequestMessageException ex) {
            LOGGER.error("ERROR - url=" + MediaServiceUrl.MEDIASTATUS.getUrl() + ", image_message=[{}] .", message, ex);
            return buildBadRequestResponse(ex.getMessage(), MediaServiceUrl.MEDIASTATUS.getUrl().toString());
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
    
}
