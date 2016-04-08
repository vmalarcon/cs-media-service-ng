package com.expedia.content.media.processing.services.dao;

import com.amazonaws.util.CollectionUtils;
import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
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
     * verifies the rooms withtin a property
     * @param outerDomain@return true if the room belongs to the property
     */
    @SuppressWarnings("unchecked")
    public Boolean roomTypeCatalogItemIdExists(OuterDomain outerDomain) {
        final List<Integer> roomIds = DomainDataUtil.getRoomIds(outerDomain);
        Boolean roomExists = Boolean.TRUE;
        if (outerDomain.getDomain().equals(Domain.LODGING) && !CollectionUtils.isNullOrEmpty(roomIds)) {
            final Map<String, Object> results = sproc.execute(Integer.parseInt(outerDomain.getDomainId()));
            final List<RoomType> roomTypes = (List<RoomType>) results.get(PropertyRoomTypeGetIDSproc.ROOM_TYPE_RESULT_SET);

            final List<Integer> roomTypeCatalogItemIds = roomTypes.stream()
                    .map(r -> r.getRoomTypeCatalogItemID())
                    .collect(Collectors.toList());
            roomExists = roomTypeCatalogItemIds.containsAll(roomIds);
        }
        return roomExists;
    }
}
