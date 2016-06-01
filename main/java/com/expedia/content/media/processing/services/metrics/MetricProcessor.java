package com.expedia.content.media.processing.services.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import lombok.NoArgsConstructor;

/**
 * Build some specific metrics.
 */
@NoArgsConstructor
public class MetricProcessor {

    private String graphiteApiUrl;
    private String graphiteApiQuery;
    private RestTemplate template;
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricProcessor.class);
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

    public MetricProcessor(RestTemplate template) {
        this.template = template;
    }

    public MetricProcessor(String graphiteApiUrl, String graphiteApiQuery) {
        this.graphiteApiQuery = graphiteApiQuery;
        this.graphiteApiUrl = graphiteApiUrl;
    }

    /**
     * Compute the up time for the whole component.
     */
    public Double getComponentUpTime(MetricQueryScope scope) throws Exception {
        return computeTime(UP_VALUE, scope, (time, target) -> {
            LOGGER.info("Uptime successfuly computed, value =[{}] scope =[{}]", time, target);
            return time;
        });
    }

    /**
     * Compute the down time for the whole component.
     */
    public Double getComponentDownTime(MetricQueryScope scope) throws Exception {
        return computeTime(DOWN_VALUE, scope, (time, target) -> {
            LOGGER.info("Downtime successfuly computed, value =[{}] scope =[{}]", time, target);
            return time;
        });
    }

    /**
     * Compute the percentage of up time for the whole component.
     */
    public Double getComponentPercentageUpTime(MetricQueryScope scope) throws Exception {
        final Double uptime = getComponentUpTime(scope);
        return uptime.equals(DOWN_VALUE) ? DOWN_VALUE : uptime / (uptime + getComponentDownTime(scope));
    }

    /**
     * Compute The percentage of down time for the whole component.
     */
    public Double getComponentPercentageDownTime(MetricQueryScope scope) throws Exception {
        return (1 - getComponentPercentageUpTime(scope));
    }

    /**
     * Compute the time for an instance. The return time could be the up or down
     * time base on the given direction.
     * 
     * @param direction. Given direction for computing. Up or Down.
     * @return Returns the computed time.
     */
    private Double computeInstanceTime(MetricInstance metric, Double direction) throws Exception {
        return metric.getMetricPoints().stream().filter(mp -> direction.equals(mp.getValue())).mapToDouble(mp -> {
            return (double) (mp.getEndTimestamp() - mp.getStartTimestamp());
        }).sum();
    }

    /**
     * Compute the time for the whole component.
     */
    private Double computeComponentTime(Double direction, String targetPeriod) throws Exception {
        final List<MetricInstance> metrics = initDataSet(targetPeriod);
        final List<Double> times = new ArrayList<>();
        for (final MetricInstance m : metrics) {
            if (DOWN_VALUE.equals(direction)) {
                for (final Long instant : m.getTimeStampList()) {
                    if (atLeastOneinstanceIsUp(instant, metrics)) {
                        times.add(DOWN_VALUE);
                    } else {
                        times.add(computeInstanceTime(m, direction));
                    }
                }
            } else {
                times.add(computeInstanceTime(m, direction));
            }
        }
        return times.stream().mapToDouble(Double::doubleValue).max().getAsDouble();
    }

    private Double computeTime(Double direction, MetricQueryScope scope, BiFunction<Double, String, Double> computeFunction) throws Exception {
        final Double time = computeComponentTime(direction, scope.getValue());
        return computeFunction.apply(time, scope.getDescription());
    }

    /**
     * Initialize the dataset.
     * 
     * @param targetPeriod target period to query.
     */
    private List<MetricInstance> initDataSet(String targetPeriod) throws Exception {
        final List<Map<String, Object>> data = getData(targetPeriod);
        return getMetrics(data);
    }

    /**
     * Convert the data fetched on graphite to a list of Metric object. We keep
     * only data belong to the current instance. Filter is done base on the
     * Instance IP address.
     * 
     * @param data Raw metrics data to convert.
     * @return Returns the list of metrics.
     */
    private List<MetricInstance> getMetrics(final List<Map<String, Object>> data) throws Exception {
        final List<MetricInstance> metrics = new ArrayList<>();
        final SortedSet<Long> timeStampList = new TreeSet<>();
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
                    timeStampList.add(x.longValue());
                }
                Collections.sort(metricPoints);

                final MetricInstance metric = MetricInstance.builder().applicationName(target[COMPONENT_INDEX]).instanceName(target[INSTANCE_INDEX])
                        .environement(target[ENVIRONMENT_INDEX]).timeStampList(timeStampList).metricPoints(metricPoints).build();
                metrics.add(metric);
            });
        }

        return metrics;
    }

    /**
     * Verify if at least one instance is up at a specific timestamp.
     * 
     * @param timeStamp given timestamp.
     * @param metrics collection of instances to verify.
     */
    private Boolean atLeastOneinstanceIsUp(Long instant, List<MetricInstance> metrics) throws Exception {
        for (final MetricInstance metric : metrics) {
            if (instanceIsUp(metric, instant)) {
                return true;
            }
        }
        return false;
    }

    private Boolean instanceIsUp(MetricInstance instance, Long timestamp) {
        return instance.getMetricPoints().stream().anyMatch(p -> p.getEndTimestamp().equals(timestamp) && UP_VALUE.equals(p.getValue()));
    }

    /**
     * Fetch the raw data from graphite.
     */
    private List<Map<String, Object>> getData(String targetPeriod) throws Exception {
        template = template == null ? new RestTemplate() : template;
        final ResponseEntity<List> response = template.getForEntity(buildUrl(targetPeriod), List.class);
        return response.getBody();
    }

    private String buildUrl(String targetPeriod) {
        final StringBuilder sb = new StringBuilder();
        sb.append(graphiteApiUrl).append(targetPeriod).append(graphiteApiQuery);
        return sb.toString();
    }
}
