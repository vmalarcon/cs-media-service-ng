package com.expedia.content.media.processing.services.metrics;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Getter;

/**
 * Encapsulate a metric object.
 */
@Builder
@Getter
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class Metric {
    private final String applicationName;
    private final String instanceName;
    private final String environement;
    private final List<MetricPoint> metricPoints;

    @Override
    public String toString() {
        final ObjectMapper mapper = new ObjectMapper();
        String s = null;
        try {
            s = mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
        }
        return s;
    }
}
