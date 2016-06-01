package com.expedia.content.media.processing.services.metrics;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(locations = "classpath:media-services.xml")
public class MetricProcessorTest {

    private List<Map<String, Object>> data;

    private static final String DATA_POINT_FIELD = "datapoints";
    private static final String TARGET_FIELD = "target";

    private MetricProcessor metricProcessor;
    @Mock
    private RestTemplate template;
    @Mock
    private ResponseEntity<List> response;

    @Before
    public void setup() throws Exception {
        data = buildSampleData();
        when(template.getForEntity(anyString(), eq(List.class))).thenReturn(response);
        when(response.getBody()).thenReturn(data);
        metricProcessor = new MetricProcessor(template);
    }

    @Test
    public void testUpTime() throws Exception {
        assertTrue(metricProcessor.getComponentUpTime(MetricQueryScope.DAILY).equals(900.0));
        assertTrue(Double.valueOf(100.0).equals(Math.rint(metricProcessor.getComponentPercentageUpTime(MetricQueryScope.DAILY) * 100)));
        
    }

    @Test
    public void testDownTime() throws Exception {
        assertTrue(metricProcessor.getComponentDownTime(MetricQueryScope.DAILY).equals(0.0));
        assertTrue(Double.valueOf(0.0).equals(Math.rint(metricProcessor.getComponentPercentageDownTime(MetricQueryScope.DAILY) * 100)));
    }

    @Test
    public void testNonArgumentConstructor() {
        final MetricProcessor metric = new MetricProcessor();
        assertNotNull(metric);
    }

    @Test
    public void testNullObject() throws Exception {
        assertNotNull(template);
        when(template.getForEntity(anyString(), eq(List.class))).thenReturn(response);
        when(response.getBody()).thenReturn(new ArrayList<Map<String, Object>>());
    }

    private List<Map<String, Object>> buildSampleData() {
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();
        map.put(TARGET_FIELD, "stats.gauges.cs-media-service.test.ip-0-0-0-0.metrics.MediaController_isAlive.Value");
        List<List<Object>> dataPoints = new ArrayList<>();
        dataPoints.add(Arrays.asList(null, 1464397800));
        dataPoints.add(Arrays.asList(1.0, 1464398100));
        dataPoints.add(Arrays.asList(1.0, 1464398400));
        dataPoints.add(Arrays.asList(1.0, 1464398700));
        map.put(DATA_POINT_FIELD, dataPoints);
        data.add(map);
        return data;
    }
}
