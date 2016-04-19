package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.validator.S3Validator;

public class URLUtil {
    private static boolean isUrlContainSpace(String url) throws Exception {
        return (url.contains(" "))? true : false;
    }

    public static String patchURL(String url) throws Exception {
        if (url.startsWith(S3Validator.S3_PREFIX) || !isUrlContainSpace(url)) {
            return url;
        } else {
            return url.replaceAll(" ","%20");
        }
    }

    private URLUtil() {
    }
}

