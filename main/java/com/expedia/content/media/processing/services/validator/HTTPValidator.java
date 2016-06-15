package com.expedia.content.media.processing.services.validator;

import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;
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
    public static ValidationStatus checkFileExists(String fileUrl){
        try {
            final HttpHead httpHead = new HttpHead(fileUrl);
            final CloseableHttpResponse response = HTTP_CLIENT.execute(httpHead);
            if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                 return checkFileIsGreaterThanZero(response);
            } else {
                return new ValidationStatus(false, "Provided fileUrl does not exist.", HttpStatus.NOT_FOUND);
            }

        } catch (IOException e) {
            LOGGER.warn("Url check failed: [{}]!", fileUrl, e);
            return new ValidationStatus(false, "Provided fileUrl does not exist.", HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Verifies if the file is not empty.
     * @param response HttpResponse got from the HTTP fileURL
     * @return true if A)Content-Length exists in the header and it is greater than 0 B)Content-Length does
     * not exist in the header (collector can only verify if the image is empty)
     * if the the response does not contain headers (NullPointerException) or the headers do not contain
     * Content-Length (IndexOutOfBoundsException) it passes so that collector gets to verify the size
     */
    @SuppressWarnings({ "PMD.AvoidCatchingNPE"})
    private static ValidationStatus checkFileIsGreaterThanZero(CloseableHttpResponse response) {
        final Header[] headers = response.getHeaders(HttpHeaders.CONTENT_LENGTH);
        final long fileSize;
        try {
            fileSize = Long.parseLong(headers[0].getValue());
        } catch (NullPointerException|IndexOutOfBoundsException e) {
            return new ValidationStatus(true, "valid", HttpStatus.OK);
        }

        return new ValidationStatus(fileSize > 0, "Provided file is 0 Bytes", HttpStatus.BAD_REQUEST);
    }
}
