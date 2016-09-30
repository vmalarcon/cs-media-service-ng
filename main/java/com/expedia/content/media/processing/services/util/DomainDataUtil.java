package com.expedia.content.media.processing.services.util;

import static org.apache.commons.lang3.StringUtils.isNumeric;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.util.StringUtils;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.util.ObjectUtils;

@SuppressWarnings("PMD.UseUtilityClass")
public class DomainDataUtil {

    private final static String ROOMID = "roomId";
    private final static String ROOMS = "rooms";
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static String LCM_MEDIA_ID_FIELD = "lcmMediaId";

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
     * returns true if there is duplicate rooms in OuterDomain
     * 
     * @param outerDomain
     * @return
     */
    public static Boolean duplicateRoomExists(OuterDomain outerDomain) {
        final List<Object> roomIds = collectRoomIds(outerDomain);
        final Set<Object> uniqueRoomIds = new HashSet<>(roomIds);
        return CollectionUtils.isNotEmpty(roomIds) && (uniqueRoomIds.size() != roomIds.size());
    }

    /**
     * Verify if the rooms field provided in the message is valid.
     * 
     * @param outerDomain domain fields.
     * @return true if the field is valid and false otherwise.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Boolean roomsFieldIsInvalid(OuterDomain outerDomain) {
        return !getInvalidRoomList(outerDomain).stream().filter(r->{
            return !r.containsKey(ROOMID);
        }).collect(Collectors.toList()).isEmpty();
    }

    /**
     * Extract the lcm mediaId from a dynamo media object.
     *
     * @param dynamoMedia provided dynamo media object.
     * @return the mediaId if exist and null otherwise.
     */
    public static String getMediaIdFromDynamo(Media dynamoMedia) throws Exception {
        final String mediaId = dynamoMedia.getLcmMediaId();
        if (isNumeric(mediaId)) {
            return mediaId;
        }
        final String domainFields = dynamoMedia.getDomainFields();
        final Map<String,Object> domainMap = StringUtils.isNullOrEmpty(domainFields) ? null : MAPPER.readValue(domainFields, Map.class);
        if (MapUtils.isEmpty(domainMap) || ObjectUtils.isEmpty(domainMap.get(LCM_MEDIA_ID_FIELD)) || !isNumeric(domainMap.get(LCM_MEDIA_ID_FIELD).toString())) {
            return null;
        }
        return domainMap.get(LCM_MEDIA_ID_FIELD).toString();
    }
    
    /**
     * Verify if the provided domainFields is a Map.
     * 
     * @param domainFields provided domainFields;
     * 
     * @return true if the provided domainFields is a Map and false otherwise.
     */
    public static Boolean domainFieldIsValid(Object domainFields) {        
        return domainFields == null || domainFields instanceof Map;
    }
        
    /**
     * Collect  roomIds which are integer  provided in the message.
     * 
     * @param rooms provided domain fields.
     * @return
     */
    public static List<Integer> collectValidFormatRoomIds(OuterDomain outerDomain) {
        return collectRoomIds(outerDomain).stream()
                .filter(room-> room == null ? false : isNumeric(room.toString()))
                .map(room->{
                    return Integer.parseInt(room.toString());
                }).collect(Collectors.toList());
    }
        
    /**
     * Collect  roomIds which are not integer provided in the message.
     * 
     * @param rooms provided domain fields.
     * @return
     */
    public static List<Object> collectMalFormatRoomIds(OuterDomain outerDomain) {
        return collectRoomIds(outerDomain).stream()
                .filter(room->room == null ? false : !isNumeric(room.toString()))
                .collect(Collectors.toList()); 
    }
    
    /**
     * Collect roomIds from OuterDomain
     * 
     * @param outerDomain
     * @return
     */
    public static List<Object> collectRoomIds(OuterDomain outerDomain) {
        final List<Map<String, Object>> rooms = getRoomList(outerDomain);
        return rooms.stream().map(room->room.get(ROOMID)).collect(Collectors.toList());
    }
    
    /**
     * Retrieve the room list from the domain fileds.
     * 
     * @param outerDomain provided domain
     * @return
     */
    private static List<Map<String, Object>> getRoomList(OuterDomain outerDomain) {
        final Object rooms = outerDomain == null ? null : outerDomain.getDomainFieldValue(ROOMS);        
        return  rooms == null ? Collections.EMPTY_LIST : (List<Map<String, Object>>) rooms;
    }
    
    /**
     * Retrieve the invalid room list from the domain fileds.
     * A room entry is invalid when it is not a Map or it is not empty and not contains no roomId key.
     * 
     * @param outerDomain provided domain
     * @return
     */
    private static List<Map<String, Object>> getInvalidRoomList(OuterDomain outerDomain) {
        final List<Map<String, Object>> allRooms = getRoomList(outerDomain);
        return allRooms.stream().filter(r->{
            return !(r instanceof Map) || (!r.isEmpty() && !r.containsKey(ROOMID));
        }).collect(Collectors.toList());
    }

}
