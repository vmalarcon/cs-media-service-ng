package com.expedia.content.media.processing.services.validator;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.expedia.content.media.processing.services.util.URLUtil;
import com.google.common.net.HttpHeaders;

/**
 * Verifies if the HTTP URL exists.
 */
public class HTTPValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPValidator.class);

    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();

    private HTTPValidator() {
    }

    /**
     * Verifies the existence of an HTTP URL file. Handles invalid URI characters in the file name and encodes them only when
     * the file name is the last part of the path.
     * 
     * @param fileUrl The file to verify.
     * @return {@code true} if the file is found; {@code false} otherwise.
     */
    public static ValidationStatus checkFileExists(String fileUrl) {
        try {
            URI fileURI;
            try {
                fileURI = new URI(fileUrl);
            } catch (URISyntaxException use) {
                if (FilenameUtils.getExtension(fileUrl).isEmpty()) {
                    throw use;
                }
                fileURI = new URI(URLUtil.normalizeURI(fileUrl));
            }
            final HttpHead httpHead = new HttpHead(fileURI);
            final CloseableHttpResponse response = HTTP_CLIENT.execute(httpHead);
            if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                return checkFileIsGreaterThanZero(response);
            } else {
                return new ValidationStatus(false, "Provided fileUrl does not exist.", ValidationStatus.NOT_FOUND);
            }
        } catch (URISyntaxException use) {
            LOGGER.warn("Url check failed: [{}]!", fileUrl, use);
            return new ValidationStatus(false, "Provided fileUrl is invalid.", ValidationStatus.INVALID);
        } catch (Exception e) {
            LOGGER.warn("Url check failed: [{}]!", fileUrl, e);
            return new ValidationStatus(false, "Provided fileUrl does not exist.", ValidationStatus.NOT_FOUND);
        }
    }

    /**
     * Verifies if the file is not empty.
     * 
     * @param response HttpResponse got from the HTTP fileURL
     * @return true if A)Content-Length exists in the header and it is greater than 0 B)Content-Length does
     *         not exist in the header (collector can only verify if the image is empty)
     *         if the the response does not contain headers (NullPointerException) or the headers do not contain
     *         Content-Length (IndexOutOfBoundsException) it passes so that collector gets to verify the size
     */
    @SuppressWarnings({"PMD.AvoidCatchingNPE"})
    private static ValidationStatus checkFileIsGreaterThanZero(CloseableHttpResponse response) {
        final Header[] headers = response.getHeaders(HttpHeaders.CONTENT_LENGTH);
        final long fileSize;
        try {
            fileSize = Long.parseLong(headers[0].getValue());
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            return new ValidationStatus(true, "valid", ValidationStatus.VALID);
        }

        return new ValidationStatus(fileSize > 0, "Provided file is 0 Bytes", ValidationStatus.ZERO_BYTES);
    }
}
