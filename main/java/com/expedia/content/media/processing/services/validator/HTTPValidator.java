package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.util.URLUtil;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Verifies if the HTTP URL exists.
 * By default (hardcoded) it waits up to 60 seconds for the connection, otherwise it returns a failed URL location.
 */
public class HTTPValidator {
    // Timeout in millis
    private static final int ONE_MINUTE = 1000 * 60;

    private static final FormattedLogger LOGGER = new FormattedLogger(HTTPValidator.class);

    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
                                                            .setConnectTimeout(ONE_MINUTE)
                                                            .setSocketTimeout(ONE_MINUTE)
                                                            .build();

    private HTTPValidator() {
    }

    /**
     * Verifies the existence of an HTTP URL file.
     * 
     * @param fileUrl The file to verify.
     * @return {@code true} if the file is found; {@code false} otherwise.
     */
    public static ValidationStatus checkFileExists(String fileUrl) {
        try {
            final URI fileURI = new URI(URLUtil.patchURL(fileUrl));
            final HttpHead httpHead = new HttpHead(fileURI);
            httpHead.setConfig(REQUEST_CONFIG);
            final CloseableHttpResponse response = HTTP_CLIENT.execute(httpHead);
            if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                return new ValidationStatus(true, "", ValidationStatus.VALID);
            } else {
                return new ValidationStatus(false, "Provided fileUrl does not exist.", ValidationStatus.NOT_FOUND);
            }
        } catch (URISyntaxException use) {
            LOGGER.warn(use, "Url check failed Url={}", fileUrl);
            return new ValidationStatus(false, "Provided fileUrl is invalid.", ValidationStatus.INVALID);
        } catch (Exception e) {
            LOGGER.warn(e, "Url check failed Url={}", fileUrl);
            return new ValidationStatus(false, "Provided fileUrl does not exist.", ValidationStatus.NOT_FOUND);
        }
    }

}
