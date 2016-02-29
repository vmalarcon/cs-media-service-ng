package com.expedia.content.media.processing.services.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.UseUtilityClass")
public class DomainDataUtil {

    private final static String ROOMID = "roomId";

    public static List<Integer> getRoomIds(Object rooms) {
        final List<Integer> roomIds = new ArrayList<>();
        final List roomsList = (List) rooms;

        for (int i = 0; i < roomsList.size(); i++) {
            final Map room = (Map)roomsList.get(i);
            roomIds.add(Integer.parseInt(room.get(ROOMID).toString()));
        }
        return roomIds;
    }

    public static String getDomianProvider(String domainProvider, Properties providerProperties) {
        final String domainProviderText = providerProperties.entrySet().stream()
                .filter(providerProperty -> ((String) providerProperty.getValue()).equalsIgnoreCase(domainProvider))
                .map(providerProperty -> (String) providerProperty.getValue())
                .collect(Collectors.joining());
        return domainProviderText;
    }
}
