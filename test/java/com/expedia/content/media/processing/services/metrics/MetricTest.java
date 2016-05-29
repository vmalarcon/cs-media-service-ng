package com.expedia.content.media.processing.services.metrics;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MetricTest {

    @Test
    public void testMetric() {
        List<MetricPoint> points = Arrays.asList(MetricPoint.builder().endTimestamp(100L).startTimestamp(30L).value(0.0).build(),
                MetricPoint.builder().endTimestamp(130L).startTimestamp(100L).value(1.0).build(),
                MetricPoint.builder().endTimestamp(30L).startTimestamp(20L).value(0.0).build());
        Collections.sort(points);
        Metric metric = Metric.builder().applicationName("cs-media-service").environement("test").instanceName("ip-0-0-0-0").metricPoints(points).build();
        assertTrue(metric.getApplicationName().equals("cs-media-service"));
        assertTrue(metric.getEnvironement().equals("test"));
        assertTrue(metric.getInstanceName().equals("ip-0-0-0-0"));

        assertTrue(metric.getMetricPoints().get(0).getEndTimestamp().equals(30L));
        assertTrue(metric.getMetricPoints().get(0).getValue().equals(0.0));
        assertTrue(metric.getMetricPoints().get(0).getStartTimestamp().equals(20L));
        assertTrue(metric.getMetricPoints().size() == 3);
    }

}
