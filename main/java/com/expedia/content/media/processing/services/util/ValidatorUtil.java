package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ValidatorUtil {

    public static void putErrorMapToList(List<Map<String, String>> list, StringBuffer errorMsg, ImageMessage imageMesage) {
        final Map<String, String> jsonMap = new TreeMap<>();
        jsonMap.put("fileName", imageMesage.getFileName());
        jsonMap.put("error", errorMsg.toString());
        list.add(jsonMap);
    }
}
