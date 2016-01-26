package com.expedia.content.media.processing.services.util;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RouterUtilTest {

    @Test
    public void testRouteReqByPercentage() {
        RouterUtil routerUtil = new RouterUtil();
        String mediaConfig = "20";
        RestClient mockRestClient = mock(RestClient.class);
        when(mockRestClient.invokeGetService("route")).thenReturn(mediaConfig);
        routerUtil.setRestClient(mockRestClient);
        routerUtil.setPercentage(5);
        routerUtil.routeAWSByPercentage(RouterUtil.DEFAULT_ROUTER_NAME);
        int counter = 0;
        for (int i = 0; i < 10000; i++) {
            boolean awsRoute = routerUtil.routeAWSByPercentage(RouterUtil.DEFAULT_ROUTER_NAME);
            if (awsRoute == true) {
                counter++;
            }
        }
    }

    @Test
    public void testRouteReqByPercentage100() {
        RouterUtil routerUtil = new RouterUtil();
        String mediaConfig = "100";
        RestClient mockRestClient = mock(RestClient.class);
        when(mockRestClient.invokeGetService("route")).thenReturn(mediaConfig);
        routerUtil.setRestClient(mockRestClient);
        routerUtil.setPercentage(5);
        routerUtil.routeAWSByPercentage(RouterUtil.DEFAULT_ROUTER_NAME);
        int counter = 0;
        for (int i = 0; i < 100; i++) {
            boolean awsRoute = routerUtil.routeAWSByPercentage(RouterUtil.DEFAULT_ROUTER_NAME);
            if (awsRoute == true) {
                counter++;
            }
        }
    }
}
