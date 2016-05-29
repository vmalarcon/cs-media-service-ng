package com.expedia.content.media.processing.services.metrics;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

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

    @Value("${cs.metrics.target}")
    private String metricTarget;
    @Value("${media.graphite.url}")
    private String environmentUrl;

    private final Map<String, Object> allData = new HashMap<>();
    private RestTemplate template;
    private InetAddress instanceIp;

    private static final Integer PUBLISH_DELAY = 30;
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

    public MetricProcessor(List<Map<String, Object>> data, RestTemplate template, InetAddress instanceIp) throws Exception {
        this.template = template;
        this.instanceIp = instanceIp;
        allData.put(DATA_FIELD, data);
        allData.put(METRICS_FIELD, getMetrics(data));
    }

    /**
     * Compute the instance up time.
     */
    public Double getInstanceUpTime() throws Exception {
        return computeInstanceTime(UP_VALUE);
    }

    /**
     * Compute the instance down time.
     */
    public Double getInstanceDownTime() throws Exception {
        return computeInstanceTime(DOWN_VALUE);
    }

    /**
     * Compute the up time for the whole component.
     */
    public Double getComponentUpTime() throws Exception {
        final List<Map<String, Object>> data = (List<Map<String, Object>>) allData.get(DATA_FIELD);
        return getComponentTime(data.size(), getInstanceUpTime(), (size, instanceTime) -> {
            return (double) (instanceTime / size);
        });
    }

    /**
     * Compute the down time for the whole component.
     */
    public Double getComponentDownTime() throws Exception {
        final List<Map<String, Object>> data = (List<Map<String, Object>>) allData.get(DATA_FIELD);
        return getComponentTime(data.size(), getInstanceDownTime(), (size, instanceTime) -> {
            return (double) (instanceTime / size);
        });
    }

    /**
     * Compute the percentage of up time for the whole component.
     */
    public Double getComponentPercentageUpTime() throws Exception {
        return getComponentPercentageTime(getComponentUpTime(), getComponentDownTime(), (componentUptime, componentDowntime) -> {
            return componentUptime / (componentUptime + componentDowntime);
        });
    }

    /**
     * Compute The percentage of down time for the whole component.
     */
    public Double getComponentPercentageDownTime() throws Exception {
        return getComponentPercentageTime(getComponentUpTime(), getComponentDownTime(), (componentUptime, componentDowntime) -> {
            return componentDowntime / (componentUptime + componentDowntime);
        });
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
     * Compute the time in percentage for the whole component.
     * 
     * @param cUptime component up time.
     * @param cDowntime component down time.
     * @param percentageFunction Applied function to select the computation direction (up or down time).
     * @return Returns the percentage time for the component.
     */
    private Double getComponentPercentageTime(Double cUptime, Double cDowntime, BiFunction<Double, Double, Double> percentageFunction) {
        return (cUptime + cDowntime) == 0 ? 0.0 : percentageFunction.apply(cUptime, cDowntime);
    }

    /**
     * Compute the time for the whole component.
     * 
     * @param size size of the data collection extracted from graphite.
     * @param instanceTime compute time for the instance.
     * @param timeFunction Applied function to select the computation direction (up or down time).
     * @return Returns the time for the component.
     */
    private Double getComponentTime(Integer size, Double instanceTime, BiFunction<Integer, Double, Double> timeFunction) throws Exception {
        return size == 0 ? 0.0 : timeFunction.apply(size, instanceTime);
    }

    /**
     * Compute the time for an instance.
     * The return time could be the up or down time base on the given direction.
     * 
     * @param direction. Given direction for computing. Up or Down.
     * @return Returns the computed time.
     */
    private Double computeInstanceTime(Double direction) throws Exception {
        final List<Metric> metrics = (List<Metric>) allData.get(METRICS_FIELD);
        return metrics.stream().map(m -> {
            return m.getMetricPoints().stream().filter(mp -> direction.equals(mp.getValue())).mapToDouble(mp -> {
                return (double) (mp.getEndTimestamp() - mp.getStartTimestamp());
            }).sum();
        }).mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Convert the data fetched on graphite to a list of Metric object.
     * We keep only data belong to the current instance.
     * Filter is done base on the Instance IP address.
     * 
     * @param data Raw metrics data to convert.
     * @return Returns the list of metrics.
     */
    private List<Metric> getMetrics(final List<Map<String, Object>> data) throws Exception {
        instanceIp = instanceIp == null ? InetAddress.getLocalHost() : instanceIp;
        final List<Metric> metrics = new ArrayList<>();
        final String ipAddress = instanceIp.getHostAddress();
        if (data != null) {
            data.stream().filter(t -> t.get(TARGET_FIELD).toString().contains(ipAddress.replace(REGEX_SEPARATOR, '-'))).forEach(t -> {
                final String[] target = StringUtils.split((String) t.get(TARGET_FIELD), REGEX_SEPARATOR);
                final List<List<Object>> dataPoints = (List<List<Object>>) t.get(DATA_POINT_FIELD);
                final List<MetricPoint> metricPoints = new ArrayList<>();
                Long startTimestamp = null;
                for (int i = 0; i < dataPoints.size(); i++) {
                    final Integer x = (Integer) dataPoints.get(i).get(X_INDEX);
                    final Double y = (Double) dataPoints.get(i).get(Y_INDEX);
                    startTimestamp = startTimestamp == null ? (long) (x - PUBLISH_DELAY) : (Integer) dataPoints.get(i - 1).get(X_INDEX);
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
        final String url = environmentUrl + metricTarget;
        final ResponseEntity<List> response = template.getForEntity(url, List.class);
        return response.getBody();
    }
}
