package com.expedia.content.media.processing.services.util;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for Data manager sercice
 */
@Component
public class DataManagerRestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataManagerRestClient.class);

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String CLIENT_ID = "MediaServices";
    private static final Charset ENCODING_CHARSET = Charset.forName("UTF-8");

    private RestTemplate restTemplate = new RestTemplate();

    private final URI uri;
    private final String environment;
    private final String proptertyName;
    private final Integer timeout;

    /**
     * Main constructor for the client
     *
     * @param url Service endpoint
     * @param environment
     * @param proptertyName
     * @param timeout Timeout for connection and response
     */
    @Autowired
    public DataManagerRestClient(@Value("${data.manager.endpoint}") String url,
            @Value("${EXPEDIA_ENVIRONMENT}") String environment, @Value("${data.manager.propertyname}") String proptertyName,
            @Value("${data.manager.timeout}") Integer timeout) {
        this.environment = environment;
        this.proptertyName = proptertyName;

        try {
            this.uri = new URL(url).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RestClientException("Invalid URL: " + url, e);
        }
        this.timeout = timeout;
        configureRestTemplate();
    }


    /**
     * Invokes the service by taking a JSON string as the body of the request
     *
     * @return Response from the service as a JSON string
     */
    public String invokeGetService() {
        try {
            final HttpHeaders headers = new HttpHeaders();
            final HttpEntity<String> httpEntity = new HttpEntity<>("", headers);
            String uriWithParm = uri.toString() + "?environment=" + environment + "&propertyName=" + proptertyName;
            URI dataManagerURL = null;
            try {
                dataManagerURL = new URL(uriWithParm).toURI();
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RestClientException("Invalid URL: " + uriWithParm, e);
            }
            final ResponseEntity<String> response = restTemplate.exchange(dataManagerURL, HttpMethod.GET, httpEntity, String.class);
            final String responseBody = response.getBody();
            LOGGER.info("Receiving response from Data manger: request-id=[{}], responseBody=[{}]", CLIENT_ID, responseBody);
            return responseBody;
        } catch (HttpClientErrorException e) {
            LOGGER.error("Received status=[{}] and error=[{}]", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RestClientException(e);
        } catch (Exception e) {
            LOGGER.error("Error calling Data Manager Service: request-id=[{}]", CLIENT_ID, e);
            throw new RestClientException(e);
        }
    }

    private String generateJsonValue(String propertyValue) {
        try {
            Map mediaConfigMap = new HashMap<>();
            mediaConfigMap.put("environment", environment);
            mediaConfigMap.put("propertyName", proptertyName);
            mediaConfigMap.put("propertyValue", propertyValue);

            return JSON.writeValueAsString(mediaConfigMap);
        } catch (IOException ex) {
            String errorMsg = "Error writing map to json";
            throw new RequestMessageException(errorMsg, ex);
        }

    }

    /**
     * invoke create servcie insert propertyValue to MediaConfig table.
     * @param propertyValue
     * @return
     */
    public String invokeCreateService(String propertyValue) {
        try {
            final HttpHeaders headers = new HttpHeaders();
            String json = generateJsonValue(propertyValue);
            final HttpEntity<String> httpEntity = new HttpEntity<>(json, headers);
            final ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, httpEntity, String.class);
            final String responseBody = response.getBody();
            LOGGER.info("Receiving response from Data manger: request-id=[{}], responseBody=[{}]", CLIENT_ID, responseBody);
            return responseBody;
        } catch (HttpClientErrorException e) {
            LOGGER.error("Received status=[{}] and error=[{}]", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return e.getResponseBodyAsString();
        } catch (Exception e) {
            LOGGER.error("Error calling MediaServices: request-id=[{}]", CLIENT_ID, e);
            return e.getMessage();
        }

    }



    /**
     * Allows for the replacement of the default RestTemplate
     * This is mostly used for testing
     *
     * @param restTemplate Rest template to use. By default this client creates one
     *                     so there is no need to provide one for a new instance if it's
     *                     not necessary.
     */
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        configureRestTemplate();
    }

    /**
     * Set the timeouts on RestTemplates's RequestFactory
     */
    private void configureRestTemplate() {
        final SimpleClientHttpRequestFactory requestFactory =
                (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
    }
}
