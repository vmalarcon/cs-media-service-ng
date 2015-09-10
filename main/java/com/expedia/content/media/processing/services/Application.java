package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.exception.ImageMessageException;
import com.expedia.content.media.processing.services.util.ImageStatusException;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <code>MPP media service</code> application.
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

    @Autowired
    private MediaServiceProcess mediaServiceProcess;

    public static void main(final String[] args) {
        new SpringApplicationBuilder().showBanner(true).sources(Application.class).run(args);
    }

    /**
     * web service interface to consume media message
     * Note that the {@code @Meter} {@code @Timer} {@code @Retryable} annotations introduce aspects from metrics-support and spring-retry
     * modules. The aspects should be applied in order, Metrics being outside (outer) and retry being inside (inner).
     *
     * @param message is json format media message,fileUrl and expedia is required.
     * @return ResponseEntity is the standard spring mvn response object
     * @throws Exception
     */
    @Meter(name = "acquireMessageCounter")
    @Timer(name = "acquireMessageTimer")
    @RequestMapping(value = "/acquireMedia", method = RequestMethod.POST)
    public ResponseEntity<String> acquireMedia(@RequestBody final String message) throws Exception {
        LOGGER.info("RECEIVED - Processing message: [{}]", message);
        try {
            ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
            ValidationStatus validationStatus = mediaServiceProcess.validateImage(imageMessage);
            if (!validationStatus.isValid()) {
                return buildBadRequestResponse(validationStatus.getMessage());
            }
            mediaServiceProcess.publishMsg(imageMessage);
            LOGGER.debug("processed message=[{}] successfully", message);
            return new ResponseEntity<>("OK", HttpStatus.OK);
        } catch (IllegalStateException ex) {
            LOGGER.error("acquireMedia failed for Json message=[{}]:", message, ex);
            return buildBadRequestResponse("JSON request format is invalid. Json message=" + message);
        } catch (ImageMessageException ex) {
            LOGGER.error("Error parsing Json message=[{}].", message, ex);
            return buildBadRequestResponse(ex.getMessage());
        }
    }

    /**
     * Builds a Bad Request response for when the incoming message fails validation.
     * Note that the {@code @Meter} {@code @Timer}  annotations introduce aspects from metrics-support
     *
     * @param validationMessage, failed message from validate.
     * @return A Bad Request response.
     */
    @Counter(name = "acquireMessageBadRequestCounter")
    public ResponseEntity<String> buildBadRequestResponse(String validationMessage) {
        return new ResponseEntity<>("Bad Request: " + validationMessage, HttpStatus.BAD_REQUEST);
    }

    /**
     * web service interface to get the media file process status.
     *
     * @param message Json format message, mediaNames is array that contains media file name.
     * @return ResponseEntity is the standard spring mvn response object
     * @throws Exception
     */
    @Meter(name = "mediaStatusCounter")
    @Timer(name = "mediaStatusTimer")
    @RequestMapping(value = "/media/v1/lateststatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity mediaStatuses(@RequestBody final String message) throws Exception {
        LOGGER.info("RECEIVED - mediaStatuses get message: [{}]", message);
        try {
            Map<String, Object> map = JSONUtil.buildMapFromJson(message);
            ValidationStatus validationStatus = mediaServiceProcess.validateMediaStatus(message);
            if (!validationStatus.isValid()) {
                return buildBadRequestResponse(validationStatus.getMessage());
            }
            String jsonResponse = mediaServiceProcess.getMediaStatusList((List<String>) map.get("mediaNames"));
            return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        } catch (ImageStatusException ex) {
            LOGGER.error("Error parsing Json message=[{}].", message, ex);
            return buildBadRequestResponse(ex.getMessage());
        }
    }
}

