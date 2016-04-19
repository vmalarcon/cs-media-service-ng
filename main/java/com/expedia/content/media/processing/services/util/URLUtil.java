package com.expedia.content.media.processing.services.util;

public class URLUtil {
    private static boolean isUrlContainSpace(String url) throws Exception {
        return (url.contains(" "))? true : false;
    }

    public static String patchURL(String url) throws Exception {
        if (isUrlContainSpace(url)) {
            return url.replaceAll(" ","%20");
        } else {
            return url;

        }

    }

    private URLUtil() {
    }
}

