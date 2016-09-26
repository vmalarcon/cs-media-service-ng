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
        final Object rooms = outerDomain.getDomainFields() == null ? null : outerDomain.getDomainFields().get(ROOMS);
        if(rooms == null){
            return false;
        }
        final List roomsList = (List) rooms;
        final List status = (List) roomsList.stream().map(r -> {
            final Map room = (Map) r;
            if (room != null) {
                return room.isEmpty() || room.containsKey(ROOMID);
            }
            return false;
        }).collect(Collectors.toList());
        return status.stream().anyMatch(s -> Boolean.FALSE.equals(s));
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
        return collectRoomIds(outerDomain).stream().filter(room->isNumeric(room.toString()))
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
        return collectRoomIds(outerDomain).stream().filter(room->!isNumeric(room.toString())).collect(Collectors.toList()); 
    }
    
    /**
     * Collect roomIds from OuterDomain
     * excludes rooms with empty roomIds.
     * 
     * @param outerDomain
     * @return
     */
    public static List<Object> collectRoomIds(OuterDomain outerDomain) {
        final List<Map<String, Object>> rooms = getRoomList(outerDomain);
        return rooms.stream().map(room->getRoomId(room)).filter(r->!r.isEmpty()).collect(Collectors.toList());
    }
    
    /**
     * Retrieve the room list from the domain fileds.
     * 
     * @param outerDomain provided domain
     * @return
     */
    private static List<Map<String, Object>> getRoomList(OuterDomain outerDomain) {
        final Object rooms = outerDomain == null ? null : outerDomain.getDomainFieldValue(ROOMS);        
        final List<Map<String, Object>> roomList = rooms == null ? Collections.EMPTY_LIST : 
            rooms instanceof List ? (List<Map<String, Object>>) rooms : Collections.EMPTY_LIST;
        return roomList;
    }
    
    /**
     * Extract the roomId value.
     * 
     * @param room
     * @return
     */
    private static String getRoomId(Map<String, Object> room) {
        final Object roomId = room.get(ROOMID);
        return roomId == null ? "" : roomId.toString();
    }
}
