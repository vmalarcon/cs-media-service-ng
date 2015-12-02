package com.expedia.content.media.processing.services.util;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertTrue;

public class RouterUtilTest {

    @Test
    public void testRouteReqByPercentage() {
        RouterUtil routerUtil = new RouterUtil();
        String mediaConfig = "[{\"environment\":\"stress\",\"propertyName\":\"route\",\"propertyValue\":\"20\"}]";
        ConfigRestClient mockConfigRestClient = mock(ConfigRestClient.class);
        when(mockConfigRestClient.invokeGetService("route")).thenReturn(mediaConfig);
        routerUtil.setConfigRestClient(mockConfigRestClient);
        routerUtil.setPercentage(5);
        routerUtil.routeAWSByPercentage();
        int counter = 0;
        for (int i = 0; i < 10000; i++) {
            boolean awsRoute = routerUtil.routeAWSByPercentage();
            if (awsRoute == true) {
                counter++;
            }
        }
        assertTrue(counter > 1800 && counter < 2200);
    }

    @Test
    public void testRouteReqByPercentage100() {
        RouterUtil routerUtil = new RouterUtil();
        String mediaConfig = "[{\"environment\":\"stress\",\"propertyName\":\"route\",\"propertyValue\":\"100\"}]";
        ConfigRestClient mockConfigRestClient = mock(ConfigRestClient.class);
        when(mockConfigRestClient.invokeGetService("route")).thenReturn(mediaConfig);
        routerUtil.setConfigRestClient(mockConfigRestClient);
        routerUtil.setPercentage(5);
        routerUtil.routeAWSByPercentage();
        int counter = 0;
        for (int i = 0; i < 100; i++) {
            boolean awsRoute = routerUtil.routeAWSByPercentage();
            if (awsRoute == true) {
                counter++;
            }
        }
        assertTrue(counter == 100);
    }
}
