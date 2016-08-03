package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.reporting.FormattedLogger;
import com.expedia.content.media.processing.services.util.URLUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Verifies if the HTTP URL exists.
 */
public class HTTPValidator {
    private static final FormattedLogger LOGGER = new FormattedLogger(HTTPValidator.class);

    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();

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
