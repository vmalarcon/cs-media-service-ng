package com.expedia.content.media.processing.services.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.UseUtilityClass")
public class DomainDataUtil {

    private final static String ROOMID = "roomId";

    /**
     * utility method used to extracts roomIds from the domainFields.rooms.
     * @param rooms
     * @return list of roomIds
     */
    public static List<Integer> getRoomIds(Object rooms) {
        final List<Integer> roomIds = new ArrayList<>();
        final List roomsList = (List) rooms;

        for (int i = 0; i < roomsList.size(); i++) {
            final Map room = (Map)roomsList.get(i);
            roomIds.add(Integer.parseInt(room.get(ROOMID).toString()));
        }
        return roomIds;
    }

    /**
     * utility method used to validate the domainProvider regardless of case-sensitivity
     * @param domainProvider
     * @param providerProperties
     * @return empty if the domainProvider is invalid, domainProvider otherwise
     */
    public static String getDomianProvider(String domainProvider, Properties providerProperties) {
        final String domainProviderText = providerProperties.entrySet().stream()
                .filter(providerProperty -> ((String) providerProperty.getValue()).equalsIgnoreCase(domainProvider))
                .map(providerProperty -> (String) providerProperty.getValue())
                .collect(Collectors.joining());
        return domainProviderText;
    }
}
