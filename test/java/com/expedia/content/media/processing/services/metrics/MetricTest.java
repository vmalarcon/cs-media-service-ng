package com.expedia.content.media.processing.services.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class MetricTest {

    private List<MetricPoint> points = new ArrayList<>();

    @Before
    public void setup() {
        points = Arrays.asList(MetricPoint.builder().endTimestamp(100L).startTimestamp(30L).value(0.0).build(),
                MetricPoint.builder().endTimestamp(130L).startTimestamp(100L).value(1.0).build(),
                MetricPoint.builder().endTimestamp(30L).startTimestamp(20L).value(0.0).build());
    }

    @Test
    public void testMetric() {
        Metric metric = Metric.builder().applicationName("cs-media-service").environement("test").instanceName("ip-0-0-0-0").metricPoints(points).build();
      
        assertTrue(metric.getApplicationName().equals("cs-media-service"));
        assertTrue(metric.getEnvironement().equals("test"));
        assertTrue(metric.getInstanceName().equals("ip-0-0-0-0"));

        assertTrue(metric.getMetricPoints().get(0).getEndTimestamp().equals(100L));
        assertTrue(metric.getMetricPoints().get(0).getValue().equals(0.0));
        assertTrue(metric.getMetricPoints().get(0).getStartTimestamp().equals(30L));
        assertTrue(metric.getMetricPoints().size() == 3);
    }
    
    @Test
    public void testMetricPoint(){
        
        assertFalse(points.get(0).getEndTimestamp().equals(30L));
        assertTrue(points.get(0).getValue().equals(0.0));
        assertFalse(points.get(0).getStartTimestamp().equals(20L));
        assertTrue(points.size() == 3);

        Collections.sort(points);
        assertTrue(points.get(0).getEndTimestamp().equals(30L));
        assertTrue(points.get(0).getValue().equals(0.0));
        assertTrue(points.get(0).getStartTimestamp().equals(20L));
    }

}
