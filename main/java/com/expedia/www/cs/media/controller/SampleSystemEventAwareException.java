package com.expedia.www.cs.media.controller;

import com.expedia.www.platform.diagnostics.systemevent.Event;
import com.expedia.www.platform.diagnostics.systemevent.EventAwareException;

public class SampleSystemEventAwareException extends EventAwareException {

    public SampleSystemEventAwareException(Event event) {
        super(event);
    }

    public SampleSystemEventAwareException(Event event, Throwable throwable) {
        super(event, throwable);
    }

    public SampleSystemEventAwareException(Event event, String message) {
        super(event, message);
    }

    public SampleSystemEventAwareException(Event event, String message, Throwable throwable) {
        super(event, message, throwable);
    }
}
