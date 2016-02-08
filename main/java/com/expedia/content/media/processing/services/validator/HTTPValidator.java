package com.expedia.content.media.processing.services.validator;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Verifies if the HTTP URL exists.
 */
public class HTTPValidator {

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
            final HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
            connection.setRequestMethod("HEAD");
            return (connection.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            return false;
        }
    }

}
