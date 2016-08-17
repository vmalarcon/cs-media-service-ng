package com.expedia.content.media.processing.services.util;

import java.util.List;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;

@SuppressWarnings("PMD.UseUtilityClass")
public class ValidatorUtil {

    public static void putErrorMapToList(List<String> list, StringBuffer errorMsg, ImageMessage imageMesage) {
        list.add(errorMsg.toString());
    }
}
