package com.expedia.content.media.processing.services.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@EnableScheduling
public class RouterUtil {

    @Value("${media.request.collector.v1.percentage}")
    private int percentage;

    @Value("${media.router.providers}")
    private String providerRouters;

    @Autowired
    RestClient restClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(RouterUtil.class);
    public static final String DEFAULT_ROUTER_NAME = "route";

    private static HashMap<String, Integer> routerValueMap = new HashMap<>();

    /**
     * get the percentage configuration value from dynamoDB web service call,
     * and compare the value the java random value to decide route to AWS queue or not.
     *
     * @return boolean indicate whether
     */
    public boolean routeAWSByPercentage(String routerName) {
        int ranNum = (int) (Math.random() * 100);
        int currentPercentValue;
        String propertyValue = "";
        if (routerValueMap.get(routerName) != null) {
            currentPercentValue = routerValueMap.get(routerName);
        } else {
            try {
                propertyValue = restClient.invokeGetService(routerName);
            } catch (RestClientException ex) {
                LOGGER.error("Error calling Data Manager Service exception", ex);
            }
            if (!StringUtils.isEmpty(propertyValue)) {
                currentPercentValue = Integer.parseInt(propertyValue);
            } else {
                restClient.createProperty(routerName, Integer.toString(percentage));
                currentPercentValue = percentage;
            }
        }
        if (ranNum < currentPercentValue) {
            return true;
        }
        return false;
    }

    /**
     * refresh the value to memory every 300 second.
     */
    @Scheduled(fixedRate = 300000)
    public void refreshCacheValue() {
        restClient.initRouterValue(routerValueMap);
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }
}
