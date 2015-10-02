package com.expedia.www.cs.media.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/service")
public class ServiceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceController.class);

    @Value("${test.helloMsg}")
	String message;

    public void setMessage(String message) {
        this.message = message;
    }

    @RequestMapping(value = "hello", method = {RequestMethod.GET, RequestMethod.POST}, produces = "application/json")
    public ResponseEntity<?> hello() {
        final HelloMessage helloMessage = new HelloMessage();
        helloMessage.setMessage(this.message);
        return new ResponseEntity<>(helloMessage, HttpStatus.OK);
    }

    @RequestMapping(value="/throw/systemevent", produces="application/json")
    @ResponseBody
    public String throwSystemEvent() {
        LOGGER.debug("To test system event");

        catchAndLogSampleSystemEventAwareException();

        return "Caught SampleSystemEventAwareException and written to the log";
    }

    private void catchAndLogSampleSystemEventAwareException() {
        try {
            throw new SampleSystemEventAwareException(SampleSystemEvent.SAMPLE_SYS_EVENT, "This is a test");
        } catch (SampleSystemEventAwareException e) {
            LOGGER.error("Caught SampleSystemEventAwareException", e);
        }
    }
}
