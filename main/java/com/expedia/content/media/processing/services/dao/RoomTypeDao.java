package com.expedia.content.media.processing.services.dao;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.util.CollectionUtils;
import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.util.DomainDataUtil;

/**
 * RoomType DAO.
 */
@Component
public class RoomTypeDao {
    private final PropertyRoomTypeGetIDSproc sprocLocalInventory;
    private final RoomTypeThirdPartyGet sprocThirdParty;

    @Autowired
    public RoomTypeDao(PropertyRoomTypeGetIDSproc sprocLocalInventory, RoomTypeThirdPartyGet sprocThirdParty) {
        this.sprocLocalInventory = sprocLocalInventory;
        this.sprocThirdParty = sprocThirdParty;
    }

    /**
     * Retrieves invalid roomIds provided in the request.
     *
     * @param outerDomain provided domainFields.
     * @return the invalid roomIds list.
     */
    public List<Object> getInvalidRoomIds(OuterDomain outerDomain) throws ClassCastException {
        final List<Object> malFormatRoomIds = DomainDataUtil.collectMalFormatRoomIds(outerDomain);
        final List<Integer> validFormatRoomIds = DomainDataUtil.collectValidFormatRoomIds(outerDomain);

        if (outerDomain.getDomain().equals(Domain.LODGING) && !CollectionUtils.isNullOrEmpty(validFormatRoomIds)) {
            final Integer domainId = Integer.parseInt(outerDomain.getDomainId());
            final Map<String, Object> results = sprocLocalInventory.execute(domainId);
            final List<RoomType> roomTypes = (List<RoomType>) results.get(PropertyRoomTypeGetIDSproc.ROOM_TYPE_RESULT_SET);

            final Map<String, Object> resultsThirdParty = sprocThirdParty.getRooms(domainId, null, null, null);
            final List<RoomType> roomThirdParty = (List<RoomType>) resultsThirdParty.get(RoomTypeThirdPartyGet.RESULTS_KEY);

            final List<Integer> roomTypeCatalogItemIds = roomTypes.stream()
                    .map(r -> r.getRoomTypeCatalogItemID())
                    .collect(Collectors.toList());
            final List<Integer> roomTypeThirdParty = roomThirdParty.stream()
                    .map(r -> r.getRoomTypeCatalogItemID())
                    .collect(Collectors.toList());
            roomTypeCatalogItemIds.addAll(roomTypeThirdParty);
            validFormatRoomIds.removeAll(roomTypeCatalogItemIds);
        }
        malFormatRoomIds.addAll(validFormatRoomIds);
        return malFormatRoomIds;
    }
}
