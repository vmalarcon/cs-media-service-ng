package com.expedia.content.media.processing.services.metrics;

import lombok.Builder;
import lombok.Getter;

/**
 * Encapsulate a metric data point.
 */
@Builder
@Getter
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class MetricPoint implements Comparable<MetricPoint> {
    private final Long startTimestamp;
    private final Long endTimestamp;
    private final Double value;

    @Override
    public int compareTo(MetricPoint o) {
        return this.endTimestamp.compareTo(o.getEndTimestamp());
    }
}
