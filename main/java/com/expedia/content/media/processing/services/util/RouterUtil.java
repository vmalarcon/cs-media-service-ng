package com.expedia.content.media.processing.services.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@EnableScheduling
public class RouterUtil {

    @Value("${media.request.collector.v1.percentage}")
    private int percentage;
    @Autowired
    DataManagerRestClient dataManagerRestClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(RouterUtil.class);

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
        String mediaConfigResponse = "[]";
        if (cachePercentValue != 0) {
            currentPercentValue = cachePercentValue;
        } else {
            try {
                mediaConfigResponse = dataManagerRestClient.invokeGetService();
            } catch (RestClientException ex) {
                LOGGER.error("Error calling Data Manager Service exception", ex);
            }
            List<Map> configMap = JSONUtil.buildMapListFromJson(mediaConfigResponse);
            if (configMap != null && configMap.size() > 0) {
                String value = (String) configMap.get(0).get("propertyValue");
                currentPercentValue = Integer.parseInt(value);
            } else {
                dataManagerRestClient.invokeCreateService(Integer.toString(percentage));
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
        String mediaConfigResponse = dataManagerRestClient.invokeGetService();
        List<Map> configMap = JSONUtil.buildMapListFromJson(mediaConfigResponse);
        if (configMap.size() > 0) {
            String value = (String) configMap.get(0).get("propertyValue");
            cachePercentValue = Integer.parseInt(value);
        }
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public void setDataManagerRestClient(DataManagerRestClient dataManagerRestClient) {
        this.dataManagerRestClient = dataManagerRestClient;
    }
}
