package com.expedia.content.media.processing.services.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RestClientTest {

    @Mock
    private RestTemplate mockTemplate;

    @Mock
    private SimpleClientHttpRequestFactory mockRequestFactory;

    @Mock
    private ResponseEntity<String> mockResponse;

    @Captor
    private ArgumentCaptor<URI> uriArgumentCaptor;

    @Captor
    private ArgumentCaptor<HttpEntity<String>> httpEntityArgumentCaptor;

    @Test
    public void testInvokeGetService() {
        when(mockTemplate.getRequestFactory()).thenReturn(mockRequestFactory);
        final RestClient client = new RestClient("http://some:80/service","test", 10000);
        client.setRestTemplate(mockTemplate);
        when(mockTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class))).thenReturn(mockResponse);
        when(mockResponse.getBody()).thenReturn("[\n"
                + "  {\n"
                + "    \"environment\": \"test\",\n"
                + "    \"propertyName\": \"route\",\n"
                + "    \"propertyValue\": \"50\"\n"
                + "  }]");

        final String response = client.invokeGetService("route");

        verify(mockTemplate).exchange(uriArgumentCaptor.capture(), eq(HttpMethod.GET), httpEntityArgumentCaptor.capture(), eq(String.class));
        verify(mockRequestFactory).setConnectTimeout(10000);
        verify(mockRequestFactory).setConnectTimeout(10000);
        final URI endPoint = uriArgumentCaptor.getValue();

        assertEquals(endPoint.toString(), "http://some:80/service?environment=test&propertyName=route");
        assertEquals("50", response);
    }

    @Test
    public void testInitRouterValue() {
        when(mockTemplate.getRequestFactory()).thenReturn(mockRequestFactory);
        final RestClient client = new RestClient("http://some:80/service","test", 10000);
        client.setRestTemplate(mockTemplate);
        when(mockTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class))).thenReturn(mockResponse);
        when(mockResponse.getBody()).thenReturn("[\n"
                + "  {\n"
                + "    \"environment\": \"test\",\n"
                + "    \"propertyName\": \"Orbitz\",\n"
                + "    \"propertyValue\": \"100\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"environment\": \"test\",\n"
                + "    \"propertyName\": \"ORB\",\n"
                + "    \"propertyValue\": \"90\"\n"
                + "  },\n"
                + "  {\n"
                + "    \"environment\": \"test\",\n"
                + "    \"propertyName\": \"route\",\n"
                + "    \"propertyValue\": \"50\"\n"
                + "  }\n"
                + "]");

        HashMap<String, Integer> routerValueMap = new HashMap<>();
        client.initRouterValue(routerValueMap);

        verify(mockTemplate).exchange(uriArgumentCaptor.capture(), eq(HttpMethod.GET), httpEntityArgumentCaptor.capture(), eq(String.class));
        verify(mockRequestFactory).setConnectTimeout(10000);
        verify(mockRequestFactory).setConnectTimeout(10000);
        final URI endPoint = uriArgumentCaptor.getValue();

        assertEquals(endPoint.toString(), "http://some:80/service?environment=test");
        assertEquals(100, routerValueMap.get("Orbitz").intValue());
        assertEquals(90, routerValueMap.get("ORB").intValue());
        assertEquals(50, routerValueMap.get("route").intValue());
    }

    @Test
         public void testInvokePostService() {
        when(mockTemplate.getRequestFactory()).thenReturn(mockRequestFactory);
        final RestClient client = new RestClient("http://some:80/service","test", 10000);
        client.setRestTemplate(mockTemplate);
        when(mockTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class))).thenReturn(mockResponse);
        when(mockResponse.getBody()).thenReturn("OK");

        final String response = client.createProperty("test","route");

        verify(mockTemplate).exchange(uriArgumentCaptor.capture(), eq(HttpMethod.POST), httpEntityArgumentCaptor.capture(), eq(String.class));
        verify(mockRequestFactory).setConnectTimeout(10000);
        verify(mockRequestFactory).setConnectTimeout(10000);
        final URI endPoint = uriArgumentCaptor.getValue();

        assertEquals("OK", response);
    }

    @Test
    public void testInvokeMeidaService() throws Exception{
        when(mockTemplate.getRequestFactory()).thenReturn(mockRequestFactory);
        final RestClient client = new RestClient("http://some:80/service","test", 10000);
        client.setRestTemplate(mockTemplate);
        when(mockTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class))).thenReturn(mockResponse);
        when(mockResponse.getBody()).thenReturn("OK");

        final String response = client.callMediaService("test");

        verify(mockTemplate).exchange(uriArgumentCaptor.capture(), eq(HttpMethod.POST), httpEntityArgumentCaptor.capture(), eq(String.class));
        verify(mockRequestFactory).setConnectTimeout(10000);
        verify(mockRequestFactory).setConnectTimeout(10000);
        final URI endPoint = uriArgumentCaptor.getValue();

        assertEquals("OK", response);
    }

}
