package com.expedia.content.media.processing.services;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class ApplicationTest {

    private static MultiValueMap<String, String> httpHeaders;
    @Autowired
    private Application application;

    @BeforeClass
    public static void init() {
        httpHeaders = new HttpHeaders();
        List<String> ids = new ArrayList<String>();
        ids.add("testid");
        httpHeaders.put("request-id", ids);
    }

    @Test
    public void testLoadContext() {
        assertNotNull(application);
    }

    @Test
    public void testAlphanumericStringExpediaIdInJsonMessage() throws Exception {
        String jsonMessage = "{  \n" +
                "   \"mediaProviderId\":\"1001\",\n" +
                "   \"fileUrl\":\"http://images.com/dir1/img1.jpg\",\n" +
                "   \"imageType\":\"Lodging\",\n" +
                "   \"stagingKey\":{  \n" +
                "      \"externalId\":\"222\",\n" +
                "      \"providerId\":\"300\",\n" +
                "      \"sourceId\":\"99\"\n" +
                "   },\n" +
                "   \"expediaId\":\"NOT_A_NUMBER\",\n" +
                "   \"categoryId\":\"801\",\n" +
                "   \"callback\":\"http://multi.source.callback/callback\",\n" +
                "   \"caption\":\"caption\"\n" +
                "}";
        ResponseEntity<?> responseEntity = application.acquireMedia(jsonMessage,httpHeaders);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testAlphanumericConstantExpediaIdInJsonMessage() throws Exception {
        String jsonMessage = "{  \n" +
                "   \"mediaProviderId\":\"1001\",\n" +
                "   \"fileUrl\":\"http://images.com/dir1/img1.jpg\",\n" +
                "   \"imageType\":\"Lodging\",\n" +
                "   \"stagingKey\":{  \n" +
                "      \"externalId\":\"222\",\n" +
                "      \"providerId\":\"300\",\n" +
                "      \"sourceId\":\"99\"\n" +
                "   },\n" +
                "   \"expediaId\":NOT_A_NUMBER,\n" +
                "   \"categoryId\":\"801\",\n" +
                "   \"callback\":\"http://multi.source.callback/callback\",\n" +
                "   \"caption\":\"caption\"\n" +
                "}";
        ResponseEntity<?> responseEntity = application.acquireMedia(jsonMessage, httpHeaders);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testWrongPropertyName() throws Exception {
        String jsonMessage = "{  \n"
                + "   \"mediaNamesaa\":[\"1037678_109010ice.jpg\",\"1055797_1742165ice.jpg\"]\n"
                + "}";

        ResponseEntity<?> responseEntity = application.getMediaLatestStatus(jsonMessage, httpHeaders);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void testWrongFormatMediaStatusMessage() throws Exception {
        String jsonMessage = "{  \n"
                + "   \"mediaNames\":\"1037678_109010ice.jpg\",\"1055797_1742165ice.jpg\"]\n"
                + "}";
        ResponseEntity<?> responseEntity = application.getMediaLatestStatus(jsonMessage, httpHeaders);
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

}
