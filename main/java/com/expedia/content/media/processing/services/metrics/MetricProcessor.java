package com.expedia.content.media.processing.services.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import lombok.NoArgsConstructor;

/**
 * Build some specific metrics.
 */
@NoArgsConstructor
public class MetricProcessor {

    @Value("${graphite.api.url}")
    private String graphiteApiUrl;
    private final Map<String, Object> allData = new HashMap<>();
    private RestTemplate template;
    private static final String DATA_POINT_FIELD = "datapoints";
    private static final String TARGET_FIELD = "target";

    private static final String DATA_FIELD = "data";
    private static final String METRICS_FIELD = "metrics";

    private static final int COMPONENT_INDEX = 2;
    private static final int ENVIRONMENT_INDEX = 3;
    private static final int INSTANCE_INDEX = 4;
    private static final char REGEX_SEPARATOR = '.';
    private static final int X_INDEX = 1;
    private static final int Y_INDEX = 0;
    private static final Double UP_VALUE = 1.0;
    private static final Double DOWN_VALUE = 0.0;
    private static final String UP_FIELD = "upTimeValues";
    private static final String DOWN_FIELD = "downTimeValues";

    public MetricProcessor(List<Map<String, Object>> data, RestTemplate template) throws Exception {
        this.template = template;
        allData.put(DATA_FIELD, data);
        allData.put(METRICS_FIELD, getMetrics(data));
    }

    /**
     * Compute the up time for the whole component.
     */
    public Double getComponentUpTime() throws Exception {
        return computeComponentTime(UP_VALUE);
    }

    /**
     * Compute the down time for the whole component.
     */
    public Double getComponentDownTime() throws Exception {
        return computeComponentTime(DOWN_VALUE);
    }

    /**
     * Compute the percentage of up time for the whole component.
     */
    public Double getComponentPercentageUpTime() throws Exception {
        final Double uptime = getComponentUpTime();
        final List<Double> uptimeList = (List<Double>) allData.get(UP_FIELD);
        final List<Double> downtimeList = (List<Double>) allData.get(DOWN_FIELD);
        componentIsUp(uptimeList, downtimeList);
        return uptime.equals(DOWN_VALUE) ? DOWN_VALUE : uptime / (uptime + getComponentDownTime());
    }

    /**
     * Compute The percentage of down time for the whole component.
     */
    public Double getComponentPercentageDownTime() throws Exception {
        return (1 - getComponentPercentageUpTime());
    }

    private Boolean componentIsUp(final List<Double> uptimeList, final List<Double> downtimeList) {
        Double time = uptimeList.stream().mapToDouble(Double::doubleValue).sum();
        return uptimeList.contains(downtimeList);
    }

    /**
     * Initialize the dataset.
     */
    @PostConstruct
    public void setAllData() throws Exception {
        final List<Map<String, Object>> data = getData();
        allData.put(DATA_FIELD, data);
        allData.put(METRICS_FIELD, getMetrics(data));
    }

    /**
     * Compute the time for an instance. The return time could be the up or down
     * time base on the given direction.
     * 
     * @param direction. Given direction for computing. Up or Down.
     * @return Returns the computed time.
     */
    private Double computeInstanceTime(Metric metric, Double direction) throws Exception {
        return metric.getMetricPoints().stream().filter(mp -> direction.equals(mp.getValue())).mapToDouble(mp -> {
            return (double) (mp.getEndTimestamp() - mp.getStartTimestamp());
        }).sum();
    }

    /**
     * Compute the time for the whole component.
     */
    private Double computeComponentTime(Double direction) throws Exception {
        final List<Metric> metrics = (List<Metric>) allData.get(METRICS_FIELD);
        final List<Double> times = new ArrayList<>();
        for (final Metric m : metrics) {
            times.add(computeInstanceTime(m, direction));
        }
        allData.put(direction.equals(UP_VALUE) ? UP_FIELD : DOWN_FIELD, times);
        return times.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
    }

    /**
     * Convert the data fetched on graphite to a list of Metric object. We keep
     * only data belong to the current instance. Filter is done base on the
     * Instance IP address.
     * 
     * @param data Raw metrics data to convert.
     * @return Returns the list of metrics.
     */
    private List<Metric> getMetrics(final List<Map<String, Object>> data) throws Exception {
        final List<Metric> metrics = new ArrayList<>();
        if (data != null) {
            data.stream().forEach(t -> {
                final String[] target = StringUtils.split((String) t.get(TARGET_FIELD), REGEX_SEPARATOR);
                final List<List<Object>> dataPoints = (List<List<Object>>) t.get(DATA_POINT_FIELD);
                final List<MetricPoint> metricPoints = new ArrayList<>();
                Long startTimestamp = null;
                for (int i = 0; i < dataPoints.size(); i++) {
                    final Integer x = (Integer) dataPoints.get(i).get(X_INDEX);
                    final Double y = (Double) dataPoints.get(i).get(Y_INDEX);
                    startTimestamp = startTimestamp == null ? (long) x : (Integer) dataPoints.get(i - 1).get(X_INDEX);
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
     * Fetch the raw data from graphite.
     */
    private List<Map<String, Object>> getData() throws Exception {
        template = template == null ? new RestTemplate() : template;
        final ResponseEntity<List> response = template.getForEntity(graphiteApiUrl, List.class);
        return response.getBody();
    }
}
