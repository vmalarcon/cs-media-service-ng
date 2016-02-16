package com.expedia.content.media.processing.services.dao;

import com.amazonaws.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RoomType DAO.
 */
@Component
public class RoomTypeDao {
    private final PropertyRoomTypeGetIDSproc sproc;

    @Autowired
    public RoomTypeDao(PropertyRoomTypeGetIDSproc sproc) {
        this.sproc = sproc;
    }


    /**
     *
     * @param pPropertyShellID domainId
     * @return true if the room belongs to the property
     */
    @SuppressWarnings("unchecked")
    public Boolean getRoomTypeCatalogItemId(int pPropertyShellID, List<Integer> roomIds) {
        final Map<String, Object> results = sproc.execute(pPropertyShellID);
        final List<RoomType> roomTypes = (List<RoomType>) results.get(PropertyRoomTypeGetIDSproc.ROOM_TYPE_RESULT_SET);

        final List<Integer> roomTypeCatalogItemIds = roomTypes.stream()
                .map(r -> r.getRoomTypeCatalogItemID())
                .collect(Collectors.toList());
        return (!CollectionUtils.isNullOrEmpty(roomIds) && roomTypeCatalogItemIds.containsAll(roomIds));
    }
}
