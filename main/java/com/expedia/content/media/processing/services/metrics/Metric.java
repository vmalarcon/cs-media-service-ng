package com.expedia.content.media.processing.services.metrics;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class Metric {
    private final String applicationName;
    private final String instanceName;
    private final String environement;
    private final List<MetricPoint> metricPoints;
}
