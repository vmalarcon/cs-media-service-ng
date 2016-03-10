package com.expedia.content.media.processing.services.validator;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

/**
 * Verifies if the HTTP URL exists.
 */
public class HTTPValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPValidator.class);

    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();

    private HTTPValidator() {
    }

    /**
     * Verifies the existence of an HTTP URL file.
     * 
     * @param fileUrl The file to verify.
     * @return {@code true} if the file is found; {@code false} otherwise.
     */
    public static boolean checkFileExists(String fileUrl) {
        try {
            final HttpHead httpHead = new HttpHead(fileUrl);
            final CloseableHttpResponse response = HTTP_CLIENT.execute(httpHead);
            return (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            LOGGER.warn("Url check failed: [{}]!", fileUrl, e);
            return false;
        }
    }

}
