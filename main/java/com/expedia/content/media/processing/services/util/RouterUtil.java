package com.expedia.content.media.processing.services.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class RouterUtil {

    @Value("${media.request.collector.v1.percentage}")
    private int percentage;
    @Autowired
    RestClient restClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(RouterUtil.class);
    private static final String PROPERTYNAME = "route";

    private static int cachePercentValue;

    /**
     * get the percentage configuration value from dynamoDB web service call,
     * and compare the value the java random value to decide route to AWS queue or not.
     *
     * @return boolean indicate whether
     */
    public boolean routeAWSByPercentage() {
        int ranNum = (int) (Math.random() * 100);
        int currentPercentValue = 0;
        String propertyValue = "";
        if (cachePercentValue != 0) {
            currentPercentValue = cachePercentValue;
        } else {
            try {
                propertyValue = restClient.invokeGetService(PROPERTYNAME);
            } catch (RestClientException ex) {
                LOGGER.error("Error calling Data Manager Service exception", ex);
            }
            if (!StringUtils.isEmpty(propertyValue)) {
                currentPercentValue = Integer.parseInt(propertyValue);
            } else {
                restClient.createProperty(PROPERTYNAME, Integer.toString(percentage));
                currentPercentValue = percentage;
            }
        }
        if (ranNum < currentPercentValue) {
            return true;
        }
        return false;
    }

    /**
     * refresh the value to memory every 30 second.
     */
    @Scheduled(fixedRate = 30000)
    public void refreshCacheValue() {
        String propertyValue = "";
        propertyValue = restClient.invokeGetService(PROPERTYNAME);
        if (!StringUtils.isEmpty(propertyValue)) {
            cachePercentValue = Integer.parseInt(propertyValue);
        }
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }
}
