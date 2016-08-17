package com.expedia.content.media.processing.services.util;

import java.util.List;

@SuppressWarnings("PMD.UseUtilityClass")
public class ValidatorUtil {

    public static void putErrorMapToList(List<String> list, StringBuffer errorMsg) {
        list.add(errorMsg.toString());
    }
}
