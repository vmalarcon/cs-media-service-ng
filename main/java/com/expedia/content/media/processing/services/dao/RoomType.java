package com.expedia.content.media.processing.services.dao;

import java.sql.Timestamp;

/**
 * Represents partially RoomType in LCM.
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class RoomType {

    private final int roomTypeCatalogItemID;
    private final int roomTypeID;
    private final Timestamp updateDate;
    private final String lastUpdatedBy;
    private final String lastUpdateLocation;

    public RoomType(int roomTypeCatalogItemID, int roomTypeID, Timestamp updateDate, String lastUpdatedBy, String lastUpdateLocation) {
        this.roomTypeCatalogItemID = roomTypeCatalogItemID;
        this.roomTypeID = roomTypeID;
        this.updateDate = updateDate;
        this.lastUpdatedBy = lastUpdatedBy;
        this.lastUpdateLocation = lastUpdateLocation;
    }

    public int getRoomTypeCatalogItemID() {
        return roomTypeCatalogItemID;
    }
}
