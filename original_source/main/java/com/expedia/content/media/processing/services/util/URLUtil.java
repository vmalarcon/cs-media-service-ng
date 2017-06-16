package com.expedia.content.media.processing.services.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.expedia.content.media.processing.services.validator.S3Validator;

/**
 *  Utility class for URL manipulation.
 */

public class URLUtil {
    
    @SuppressWarnings({"serial", "PMD.NonStaticInitializer"})
    private static final Map<Character, String> ENCODABLES = new HashMap<Character, String>(){{put(' ',"%20"); put('[', "%5B"); put(']', "%5D");}};

    private URLUtil() {

    }

    /**
     * Utility method used to replace some special characters in URL with encoded version. 
     *
     * @param url URL to be patched.
     * @return patched URL with special characters replaced to their encoded value.
     */
    public static String patchURL(String url){
        if (url.startsWith(S3Validator.S3_PREFIX)) {
            return url;
        } else {
            return url.chars().mapToObj(c -> (char) c).map(c -> ENCODABLES.getOrDefault(c, String.valueOf(c))).collect(Collectors.joining());
        }
    }

}

