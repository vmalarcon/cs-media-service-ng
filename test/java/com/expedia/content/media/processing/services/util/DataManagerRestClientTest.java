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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DataManagerRestClientTest {

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
    public void testinvokeGetService() {
        when(mockTemplate.getRequestFactory()).thenReturn(mockRequestFactory);
        final DataManagerRestClient client = new DataManagerRestClient("http://some:80/service","test", "route", 10000);
        client.setRestTemplate(mockTemplate);
        when(mockTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class))).thenReturn(mockResponse);
        when(mockResponse.getBody()).thenReturn("OK");

        final String response = client.invokeGetService();

        verify(mockTemplate).exchange(uriArgumentCaptor.capture(), eq(HttpMethod.GET), httpEntityArgumentCaptor.capture(), eq(String.class));
        verify(mockRequestFactory).setConnectTimeout(10000);
        verify(mockRequestFactory).setConnectTimeout(10000);
        final URI endPoint = uriArgumentCaptor.getValue();

        assertEquals(endPoint.toString(), "http://some:80/service?environment=test&propertyName=route");
        assertEquals("OK", response);
    }

    @Test
    public void testinvokePostService() {
        when(mockTemplate.getRequestFactory()).thenReturn(mockRequestFactory);
        final DataManagerRestClient client = new DataManagerRestClient("http://some:80/service","test", "route", 10000);
        client.setRestTemplate(mockTemplate);
        when(mockTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class))).thenReturn(mockResponse);
        when(mockResponse.getBody()).thenReturn("OK");

        final String response = client.invokeCreateService("route");

        verify(mockTemplate).exchange(uriArgumentCaptor.capture(), eq(HttpMethod.POST), httpEntityArgumentCaptor.capture(), eq(String.class));
        verify(mockRequestFactory).setConnectTimeout(10000);
        verify(mockRequestFactory).setConnectTimeout(10000);
        final URI endPoint = uriArgumentCaptor.getValue();

        assertEquals("OK", response);
    }

}
