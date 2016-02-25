package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("PMD.UseUtilityClass")
public class ValidatorUtil {

    private final static String ROOMID = "roomId";

    public static void putErrorMapToList(List<Map<String, String>> list, StringBuffer errorMsg, ImageMessage imageMesage) {
        final Map<String, String> jsonMap = new TreeMap<>();
        jsonMap.put("fileName", imageMesage.getFileName());
        jsonMap.put("error", errorMsg.toString());
        list.add(jsonMap);
    }

    public static List<Integer> getRoomIds(Object rooms) {
        final List<Integer> roomIds = new ArrayList<>();
        final List roomsList = (List) rooms;

            for (int i = 0; i < roomsList.size(); i++) {
                final Map room = (Map)roomsList.get(i);
                roomIds.add(Integer.parseInt(room.get(ROOMID).toString()));
            }
        return roomIds;
    }

    public static Boolean containsIgnoreCase(Collection collection, String str) {
        final Predicate<Object> predicate  = (s) -> StringUtils.equalsIgnoreCase(s.toString(), str);
        return IterableUtils.matchesAny(collection, predicate);
    }
}
