package com.expedia.content.media.processing.services.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.io.FilenameUtils;

import com.expedia.content.media.processing.services.validator.S3Validator;

import lombok.SneakyThrows;

/**
 *  Utility class for URL manipulation.
 */

public class URLUtil {

    private URLUtil() {

    }

    /**
     * utility method used check if the URL contains space characters.
     *
     * @param url url to be checked.
     * @return true if URL contains space else false.
     */
    private static boolean isUrlContainSpace(String url) {
        return (url.contains(" "))? true : false;
    }

    /**
     * utility method used to replace space characters in URL with encoded space "%20".
     *
     * @param url url to be patched.
     * @return patched URL with space replaced to "%20".
     */
    public static String patchURL(String url){
        if (url.startsWith(S3Validator.S3_PREFIX) || !isUrlContainSpace(url)) {
            return url;
        } else {
            return url.replaceAll(" ","%20");
        }
    }

    /**
     * Normalizes the file name within a URI. Special characters in the file name (final part of the URL before the request
     * parameters) are encoded into valid strings for URIs. Occasionally some URL contain special characters and the image
     * is downloadable but the Java URI class is strict with the characters that are allowed. Will not encode characters
     * that are not part of the file name.
     *
     * Note on sneaky throws; UTF-8 is ALWAYS part of the JRE/JDK. UnsupportedEncodingException will never be thrown on UTF-8.
     *
     * @param url The URL to normalize.
     * @return The normalized URL string.
     */
    @SneakyThrows(UnsupportedEncodingException.class)
    public static String normalizeURI(String url) {
        return url.substring(0, url.lastIndexOf('/') + 1) + URLEncoder.encode(FilenameUtils.getBaseName(url), "UTF-8")
        + FilenameUtils.EXTENSION_SEPARATOR + FilenameUtils.getExtension(url);
    }
}

