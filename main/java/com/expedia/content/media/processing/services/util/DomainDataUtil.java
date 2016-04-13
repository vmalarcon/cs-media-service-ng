package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.UseUtilityClass")
public class DomainDataUtil {

    private final static String ROOMID = "roomId";
    private final static String ROOMS = "rooms";

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
            if (room != null && !room.isEmpty()) {
                roomIds.add(Integer.parseInt(room.get(ROOMID).toString()));
            }
        }
        return roomIds;
    }

    /**
     * utility method used to validate the domainProvider regardless of case-sensitivity
     * @param domainProvider
     * @param providerProperties
     * @return empty if the domainProvider is invalid, domainProvider otherwise
     */
    public static String getDomainProvider(String domainProvider, Properties providerProperties) {
        final String domainProviderText = providerProperties.entrySet().stream()
                .filter(providerProperty -> ((String) providerProperty.getValue()).equalsIgnoreCase(domainProvider))
                .map(providerProperty -> (String) providerProperty.getValue())
                .collect(Collectors.joining());
        return domainProviderText;
    }

    /**
     * extracts roomIds from OuterDomain
     * @param outerDomain
     * @return
     */
    public static List<Integer> getRoomIds(OuterDomain outerDomain) {
        final List<Integer> roomIds = outerDomain.getDomainFields() == null ||
                outerDomain.getDomainFields().get(ROOMS) == null ? Collections.EMPTY_LIST :
                DomainDataUtil.getRoomIds(outerDomain.getDomainFields().get(ROOMS));
        return roomIds;
    }

    /**
     * returns true if there is duplicate rooms in OuterDomain
     * @param outerDomain
     * @return
     */
    public static Boolean duplicateRoomExists(OuterDomain outerDomain) {
        final List<Integer> roomIds = getRoomIds(outerDomain);
        final Set<Integer> uniqueRoomIds = new HashSet<>();
        uniqueRoomIds.addAll(roomIds);
        return  CollectionUtils.isNotEmpty(roomIds) && (uniqueRoomIds.size() != roomIds.size());
    }
}
