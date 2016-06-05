package com.expedia.content.media.processing.services.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Build some specific metrics.
 */
public class MetricProcessor {

    private String graphiteApiUrl;
    private String graphiteApiQuery;
    private RestTemplate template;
    final private Map<String, Object> allData = new HashMap<>();

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
    private static final String LAST_QUERY_TIME = "lastQueryTime";
    private static final int DEFAULT_QUERY_DELAY = 30;

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
    public Integer getComponentUpTime(MetricQueryScope scope) throws Exception {
        return computeTime(UP_VALUE, scope, (time, target) -> {
            LOGGER.info("Uptime successfuly computed, value =[{}] scope =[{}]", time, target);
            return time;
        });
    }

    /**
     * Compute the down time for the whole component.
     */
    public Integer getComponentDownTime(MetricQueryScope scope) throws Exception {
        return computeTime(DOWN_VALUE, scope, (time, target) -> {
            LOGGER.info("Downtime successfuly computed, value =[{}] scope =[{}]", time, target);
            return time;
        });
    }

    /**
     * Compute the percentage of up time for the whole component.
     */
    public Double getComponentPercentageUpTime(MetricQueryScope scope) throws Exception {
        final Double uptime = getComponentUpTime(scope).doubleValue();
        final Double downtime = getComponentDownTime(scope).doubleValue();
        final Double upercentage = uptime.equals(DOWN_VALUE) ? DOWN_VALUE : uptime / (uptime + downtime);
        LOGGER.info("Uptime percentage successfuly computed, value =[{}] scope =[{}]", upercentage, scope.getDescription());
        return upercentage;
    }

    /**
     * Compute The percentage of down time for the whole component.
     */
    public Double getComponentPercentageDownTime(MetricQueryScope scope) throws Exception {
        final Double dpercentage = (1 - getComponentPercentageUpTime(scope));
        LOGGER.info("Downtime percentage successfuly computed, value =[{}] scope =[{}]", dpercentage, scope.getDescription());
        return dpercentage;
    }

    /**
     * Compute the time the whole component. The return time could be the up or down
     * time base on the given direction.
     * 
     * @param direction. Given direction for computing. Up or Down.
     * @param scope graphite query period.
     * @return Returns the computed time.
     */
    @SuppressWarnings("unchecked")
    private Integer computeComponentTime(Double direction, MetricQueryScope scope) throws Exception {
        List<MetricInstance> instances = (List<MetricInstance>) allData.get(scope.getDescription());
        final Long lastQueryTime = (Long) allData.get(LAST_QUERY_TIME);

        final Long nextQueryTime = DateTime.now().minusSeconds(DEFAULT_QUERY_DELAY).getMillis();
        if (instances == null || (lastQueryTime != null && nextQueryTime > lastQueryTime)) {
            instances = initDataSet(scope);
        }
        final List<MetricPoint> points = filterPoints(instances, direction);
        return points.size();
    }

    /**
     * Filter all the points where the component is down or up.
     * base on the given direction.
     * 
     * @param instances instances of the component
     * @param direction Given direction for filtering. Up or Down.
     * @return Return all the points where the component is down or up.
     */
    private List<MetricPoint> filterPoints(List<MetricInstance> instances, Double direction) {
        final Boolean up = UP_VALUE.equals(direction) ? true : false;
        final MetricInstance instance = instances.get(0);
        return instance.getMetricPoints().stream()
                .filter(p -> up ? componentIsUp(p.getEndTimestamp(), instances) : !componentIsUp(p.getEndTimestamp(), instances))
                .collect(Collectors.toList());
    }

    /**
     * Compute the generic time.
     * 
     * @param direction computing direction (UP or DOWN)
     * @param scope graphite query period.
     * @param computeFunction function encapsulated the up or down logic computing.
     * @return Returns the computed time.
     */
    private Integer computeTime(Double direction, MetricQueryScope scope, BiFunction<Integer, String, Integer> computeFunction) throws Exception {
        final Integer time = computeComponentTime(direction, scope);
        return computeFunction.apply(time, scope.getDescription());
    }

    /**
     * Initialize the dataset.
     * 
     * @param scope target period to query.
     */
    private List<MetricInstance> initDataSet(MetricQueryScope scope) throws Exception {
        final List<Map<String, Object>> data = getData(scope);
        return getInstances(data, scope);
    }

    /**
     * Convert the data fetched on graphite to a list of Instances.
     * 
     * @param data Raw metrics data to convert.
     * @param scope graphite query period.
     * @return Returns the list of instances.
     */
    @SuppressWarnings("unchecked")
    private List<MetricInstance> getInstances(final List<Map<String, Object>> data, MetricQueryScope scope) throws Exception {
        final List<MetricInstance> instances = new ArrayList<>();
        if (data != null) {
            data.stream().forEach(t -> {
                final String[] target = StringUtils.split((String) t.get(TARGET_FIELD), REGEX_SEPARATOR);
                final List<List<Object>> dataPoints = (List<List<Object>>) t.get(DATA_POINT_FIELD);
                final List<MetricPoint> metricPoints = new ArrayList<>();
                Long startTimestamp = null;
                for (int i = 0; i < dataPoints.size(); i++) {
                    final Integer x = (Integer) dataPoints.get(i).get(X_INDEX);
                    final Double y = (Double) dataPoints.get(i).get(Y_INDEX);
                    startTimestamp = startTimestamp == null ? (long) (x - DEFAULT_QUERY_DELAY) : (Integer) dataPoints.get(i - 1).get(X_INDEX);
                    final MetricPoint metricPoint =
                            MetricPoint.builder().startTimestamp(startTimestamp).endTimestamp(x.longValue()).value(y == null ? 0 : y).build();
                    metricPoints.add(metricPoint);
                }
                Collections.sort(metricPoints);

                final MetricInstance metric = MetricInstance.builder().applicationName(target[COMPONENT_INDEX]).instanceName(target[INSTANCE_INDEX])
                        .environement(target[ENVIRONMENT_INDEX]).metricPoints(metricPoints).build();
                instances.add(metric);
            });
        }
        allData.put(scope.getDescription(), instances);
        allData.put(LAST_QUERY_TIME, DateTime.now().getMillis());
        return instances;
    }

    /**
     * Verify if at least one instance is up at a specific timestamp.
     * 
     * @param timeStamp given timestamp.
     * @param instances collection of instances to verify.
     */
    private Boolean componentIsUp(Long instant, List<MetricInstance> instances) {
        return instances.stream().anyMatch(instance -> instanceIsUp(instance, instant));
    }

    private Boolean instanceIsUp(MetricInstance instance, Long timestamp) {
        return instance.getMetricPoints().stream().filter(p -> p.getEndTimestamp().equals(timestamp)).anyMatch(p -> UP_VALUE.equals(p.getValue()));
    }

    /**
     * Fetch the raw data from graphite.
     * 
     * @param scope graphite query period.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map<String, Object>> getData(MetricQueryScope scope) throws Exception {
        template = template == null ? new RestTemplate() : template;
        final ResponseEntity<List> response = template.getForEntity(buildUrl(scope.getValue()), List.class);
        return response.getBody();
    }

    /**
     * Build the Rest call url.
     * 
     * @param targetPeriod period to query.
     */
    private String buildUrl(String targetPeriod) {
        final StringBuilder sb = new StringBuilder();
        sb.append(graphiteApiUrl).append(targetPeriod).append(graphiteApiQuery);
        return sb.toString();
    }
}
