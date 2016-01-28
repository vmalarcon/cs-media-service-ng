package com.expedia.content.media.processing.services.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Rest Client for query Configuration service in data manager.
 * now the web service interface exist in data manager, later may move to media service
 */
@Component
public class RestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CLIENT_ID = "MediaServices";

    private RestTemplate restTemplate = new RestTemplate();

    private final URI uri;
    private final String environment;
    private final Integer timeout;

    /**
     * Main constructor for the client
     *
     * @param url         Service endpoint
     * @param environment
     * @param timeout     Timeout for connection and response
     */

    public RestClient(String url,
            String environment,
            Integer timeout) {
        this.environment = environment;

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
     * @param propertyName default value "route", can be provider name.
     *
     * @return Response from the service as a JSON string
     */
    public String invokeGetService(String propertyName) {
        String propertyValue = "";
        try {
            final String uriWithParm = uri.toString() + "?environment=" + environment + "&propertyName=" + propertyName;
            final List<Map> configMap = initConfigMap(uriWithParm);
            if (configMap != null && !configMap.isEmpty()) {
                propertyValue = (String) configMap.get(0).get("propertyValue");
            }
            return propertyValue;
        } catch (HttpClientErrorException e) {
            LOGGER.error("Received status=[{}] and error=[{}]", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RestClientException(e);
        } catch (Exception e) {
            LOGGER.error("Error calling Data Manager Service: request-id=[{}]", CLIENT_ID, e);
            throw new RestClientException(e);
        }
    }

    private List<Map> initConfigMap(final String uriWithParm) {
        URI dataManagerURL = null;
        try {
            dataManagerURL = new URL(uriWithParm).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RestClientException("Invalid URL: " + uriWithParm, e);
        }
        final HttpHeaders headers = new HttpHeaders();
        final HttpEntity<String> httpEntity = new HttpEntity<>("", headers);
        final ResponseEntity<String> response = restTemplate.exchange(dataManagerURL, HttpMethod.GET, httpEntity, String.class);
        final String responseBody = response.getBody();
        LOGGER.info("Receiving response from Data manger: request-id=[{}], responseBody=[{}]", CLIENT_ID, responseBody);
        return JSONUtil.buildMapListFromJson(responseBody);
    }

    /**
     * Invokes the service to init route configuration
     * @param routerValueMap store router name and value.
     *
     * @return Response from the service as a JSON string
     */
    public void initRouterValue(Map<String, Integer> routerValueMap) {
        try {
            final String uriWithParm = uri.toString() + "?environment=" + environment;
            final List<Map> configMap = initConfigMap(uriWithParm);
            if (configMap != null && !configMap.isEmpty()) {
                configMap.forEach(item -> {
                    routerValueMap.put((String) item.get("propertyName"), Integer.valueOf((String) item.get("propertyValue")));
                });
            }
        } catch (HttpClientErrorException e) {
            LOGGER.error("Received status=[{}] and error=[{}]", e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            LOGGER.error("Error calling Data Manager Service: request-id=[{}]", CLIENT_ID, e);
        }
    }

    private String generateJsonValue(String propertyValue, String proptertyName) {
        try {
            final Map mediaConfigMap = new HashMap<>();
            mediaConfigMap.put("environment", environment);
            mediaConfigMap.put("propertyName", proptertyName);
            mediaConfigMap.put("propertyValue", propertyValue);

            return JSON.writeValueAsString(mediaConfigMap);
        } catch (IOException ex) {
            final String errorMsg = "Error writing map to json";
            throw new RequestMessageException(errorMsg, ex);
        }

    }

    /**
     * invoke create service insert propertyValue to MediaConfig table.
     *
     * @param propertyValue
     * @return
     */
    public String createProperty(String propertyName, String propertyValue) {
        try {
            final HttpHeaders headers = new HttpHeaders();
            final String json = generateJsonValue(propertyValue, propertyName);
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
     * invoke media service acquire media method.
     *
     * @param message json format imageMessage
     * @return
     */
    public String callMediaService(String message) throws Exception{
        try {
            final HttpHeaders headers = new HttpHeaders();
            final HttpEntity<String> httpEntity = new HttpEntity<>(message, headers);
            final ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, httpEntity, String.class);
            final String responseBody = response.getBody();
            LOGGER.info("Receiving response from media service: request-id=[{}], responseBody=[{}]", CLIENT_ID, responseBody);
            return responseBody;
        } catch (HttpClientErrorException e) {
            LOGGER.error("Received status=[{}] and error=[{}]", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error calling MediaServices: request-id=[{}]", CLIENT_ID, e);
            throw e;
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
