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
     * 
     * @param rooms
     * @return list of roomIds
     */
    public static List<Integer> getRoomIds(Object rooms) {
        final List<Integer> roomIds = new ArrayList<>();
        final List roomsList = (List) rooms;

        for (int i = 0; i < roomsList.size(); i++) {
            final Map room = (Map) roomsList.get(i);
            if (room != null && !room.isEmpty() && room.containsKey(ROOMID)) {
                roomIds.add(Integer.valueOf(room.get(ROOMID).toString()));
            }
        }
        return roomIds;
    }

    /**
     * utility method used to validate the domainProvider regardless of case-sensitivity
     * 
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
     * 
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
     * 
     * @param outerDomain
     * @return
     */
    public static Boolean duplicateRoomExists(OuterDomain outerDomain) {
        final List<Integer> roomIds = getRoomIds(outerDomain);
        final Set<Integer> uniqueRoomIds = new HashSet<>(roomIds);
        return CollectionUtils.isNotEmpty(roomIds) && (uniqueRoomIds.size() != roomIds.size());
    }

    /**
     * Verify if the rooms field provided in the message is valid.
     * 
     * @param rooms rooms field provided.
     * @return true if the field is valid and false otherwise.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Boolean roomsFieldIsInvalid(OuterDomain outerDomain) {
        final Object rooms = outerDomain.getDomainFields() == null ? null : outerDomain.getDomainFields().get(ROOMS);
        if(rooms == null){
            return false;
        }
        final List roomsList = (List) rooms;
        final List<Boolean> status = (List<Boolean>) roomsList.stream().map(r -> {
            final Map room = (Map) r;
            if (room != null && !room.isEmpty()) {
                return room.containsKey(ROOMID);
            }
            return false;
        }).collect(Collectors.toList());
        return roomsList.isEmpty() || status.stream().anyMatch(s -> Boolean.FALSE.equals(s));
    }
}
