package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.domain.ImageMessage;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import com.expedia.content.metrics.aspects.EnableMonitoringAspects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;

/**
 * <code>MPP media service</code> application.
 * This class has the main Spring configuration and also the bootstrap for the application.
 */
@Configuration
@ComponentScan(basePackages = "com.expedia.content.media.processing")
@ImportResource("classpath:media-services.xml")
@RestController
@EnableAutoConfiguration
//@EnableMonitoringAspects
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    @Autowired
    private MediaServiceProcess mediaServiceProcess;

    public static void main(final String[] args) {
        new SpringApplicationBuilder().showBanner(true).sources(Application.class).run(args);
    }

    @RequestMapping(value = "/acquireMedia", method = RequestMethod.POST)
    public ResponseEntity<?> acquireMedia(@RequestBody final String message) throws Exception {
        LOGGER.debug("RECEIVED - Processing message: [{}]", message);
        try {
            ValidationStatus validationStatus = mediaServiceProcess.validateImage(message);
            if (validationStatus.isStatus() == false) {
                return new ResponseEntity<>("Bad Request " + validationStatus.getMessage(), HttpStatus.BAD_REQUEST);
            }
            mediaServiceProcess.disPatchMsg(message);
            LOGGER.debug("process message successfully");
            return new ResponseEntity<>("OK", HttpStatus.OK);
        } catch (IllegalStateException e) {
            LOGGER.error("acquireMeda fail:",e);
            //this means that json message is not right
            return new ResponseEntity<>("Bad Request json request format is not right.", HttpStatus.BAD_REQUEST);
        }

    }
}

