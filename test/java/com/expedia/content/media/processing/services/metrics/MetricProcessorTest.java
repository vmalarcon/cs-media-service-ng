package com.expedia.content.media.processing.services.metrics;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
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
import org.springframework.web.client.RestTemplate;;

@RunWith(MockitoJUnitRunner.class)
public class MetricProcessorTest {

    private List<Map<String, Object>> data;
    
    private static final String DATA_POINT_FIELD = "datapoints";
    private static final String TARGET_FIELD = "target";
    private static final String IP_ADDRESS = "0.0.0.0";
    private MetricProcessor metricProcessor;
    
    @Mock
    private RestTemplate template;
    @Mock
    private ResponseEntity<List> response;
    @Mock
    InetAddress inetAddress;
    
    @Before
    public void setup() throws Exception {
        metricProcessor = new MetricProcessor();
        data = buildSampleData();      
        when(inetAddress.getHostAddress()).thenReturn(IP_ADDRESS);
        when(template.getForEntity(anyString(), eq(List.class))).thenReturn(response);
        when(response.getBody()).thenReturn(data);
    }

    @Test
    public void testUpTime() throws Exception{
        assertTrue(metricProcessor.getUptime().equals(30L));
    }

    @Test
    public void testDownTime() throws Exception {
        assertTrue(metricProcessor.getDownTime().equals(300L));
    }

    private List<Map<String, Object>> buildSampleData() {
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();
        map.put(TARGET_FIELD, "stats.gauges.cs-media-service.test.ip-0-0-0-0.metrics.MediaController_isAlive.Value");
        List<List<Object>> dataPoints = new ArrayList<>();
        dataPoints.add(Arrays.asList(null, 1464357000));
        dataPoints.add(Arrays.asList(1.0, 1464357300));
        map.put(DATA_POINT_FIELD, dataPoints);
        data.add(map);
        return data;
    }
}
