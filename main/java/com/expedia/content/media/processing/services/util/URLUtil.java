package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.validator.S3Validator;

/**
 *  Utility class for URL manipulation.
 */

public class URLUtil {

    private URLUtil() {

    }

    /**
     * utility method used check if the URL contains space characters
     *
     * @param url url to be checked.
     * @return
     */
    private static boolean isUrlContainSpace(String url) {
        return (url.contains(" "))? true : false;
    }

    /**
     *
     * @param url url to be patched
     * @return
     */
    public static String patchURL(String url){
        if (url.startsWith(S3Validator.S3_PREFIX) || !isUrlContainSpace(url)) {
            return url;
        } else {
            return url.replaceAll(" ","%20");
        }
    }
}

