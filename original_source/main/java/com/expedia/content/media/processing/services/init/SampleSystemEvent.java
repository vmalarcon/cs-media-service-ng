package com.expedia.content.media.processing.services.init;

import com.expedia.www.platform.diagnostics.systemevent.Event;

public enum SampleSystemEvent implements Event {

        SAMPLE_SYS_EVENT(1, "Sample system event");

    private final int id;

    private final String description;

    SampleSystemEvent(int id, String description) {
        this.id = id;
        this.description = description;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name();
    }

}
