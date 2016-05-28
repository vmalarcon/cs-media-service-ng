package com.expedia.content.media.processing.services.metrics;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * 
 * This Class 
 *
 */
public class MetricProcessor {

    @Value("${cs.metrics.target}")
    private String metricTarget;
    @Value("${media.graphite.url}")
    private String environmentUrl;
    @Value("${cs.metrics.publisher.delay}")
    private Integer metricDelay;

    private static final String DATA_POINT_FIELD = "datapoints";
    private static final String TARGET_FIELD = "target";
    private static final int COMPONENT_INDEX = 2;
    private static final int ENVIRONMENT_INDEX = 3;
    private static final int INSTANCE_INDEX = 4;
    private static final char REGEX_SEPARATOR = '.';
    private static final int X_INDEX = 1;
    private static final int Y_INDEX = 0;
    private static final Double UP_VALUE = 1.0;
    private static final Double DOWN_VALUE = 0.0;

    /**
     * Compute the instance up time.
     * 
     * @return Returns the instance up time.
     */
    public Long getUptime() throws Exception {
        return computeTime(UP_VALUE);
    }

    /**
     * Compute the instance down time.
     * 
     * @return Returns the instance down time.
     */
    public Long getDownTime() throws Exception {
        return computeTime(DOWN_VALUE);
    }

    /**
     * Compute the time based on fetched data.
     * The return time could be the up or down time base on the given direction.
     * 
     * @param direction. Given direction for computing. Up or Down.
     * @return Returns the computed time.
     */
    private Long computeTime(Double direction) throws Exception {
        return getMetrics(getData()).stream().map(m -> {
            return m.getMetricPoints().stream().filter(mp -> direction.equals(mp.getValue())).mapToLong(mp -> {
                return mp.getEndTimestamp() - mp.getStartTimestamp();
            }).sum();
        }).mapToLong(Long::longValue).sum();
    }

    /**
     * Convert the data fetched on graphite to a list of Metric object.
     * We keep only data belong to the current instance.
     * Filter is done base on the Instance IP address.
     * 
     * @param responses Raw metrics data to convert.
     * @return Returns the list of metrics.
     */
    private List<Metric> getMetrics(final List<Map<String, Object>> responses) throws Exception {
        final List<Metric> metrics = new ArrayList<>();
        final String ipAddress = InetAddress.getLocalHost().getHostAddress();
        if (responses != null) {
            responses.stream()
            .filter(t -> t.get(TARGET_FIELD).toString().contains(ipAddress.replace('.', '-')))
            .forEach(t -> {
                final String[] target = StringUtils.split((String) t.get(TARGET_FIELD), REGEX_SEPARATOR);
                final List<List<Object>> dataPoints = (List<List<Object>>) t.get(DATA_POINT_FIELD);
                final List<MetricPoint> metricPoints = new ArrayList<>();
                Long startTimestamp = null;
                for (int i = 0; i < dataPoints.size(); i++) {
                    final Integer x = (Integer) dataPoints.get(i).get(X_INDEX);
                    final Double y = (Double) dataPoints.get(i).get(Y_INDEX);
                    startTimestamp = startTimestamp == null ? (long) (x - metricDelay) : (Integer) dataPoints.get(i - 1).get(X_INDEX);
                    final MetricPoint metricPoint =
                            MetricPoint.builder().startTimestamp(startTimestamp).endTimestamp(x.longValue()).value(y == null ? 0 : y).build();
                    metricPoints.add(metricPoint);
                }
                final Metric metric = Metric.builder().applicationName(target[COMPONENT_INDEX]).instanceName(target[INSTANCE_INDEX])
                        .environement(target[ENVIRONMENT_INDEX]).metricPoints(metricPoints).build();
                metrics.add(metric);
            });
        }
        return metrics;
    }

    /**
     * Fetch the raw metrics data from graphite.
     */
    private List<Map<String, Object>> getData() throws Exception {
        final String url = environmentUrl + metricTarget;
        final RestTemplate template = new RestTemplate();
        final ResponseEntity<List> response = template.getForEntity(url, List.class);
        return response.getBody();
    }
}
