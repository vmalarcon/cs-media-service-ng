package com.expedia.content.media.processing.services.dao;

import java.sql.Timestamp;

/**
 * Represents the MediaProvider in LCM.
 */
@SuppressWarnings("PMD")
public final class MediaProvider {

    private final int mediaProviderID;
    private final String mediaProviderName;
    private final Timestamp updateDate;
    private final String lastUpdatedBy;
    private final String updateLocation;

    public MediaProvider(int mediaProviderID, String mediaProviderName, Timestamp updateDate, String lastUpdatedBy, String updateLocation) {
        this.mediaProviderID = mediaProviderID;
        this.mediaProviderName = mediaProviderName;
        this.updateDate = updateDate;
        this.lastUpdatedBy = lastUpdatedBy;
        this.updateLocation = updateLocation;
    }

    public int getMediaProviderID() {
        return mediaProviderID;
    }

    public String getMediaProviderName() {
        return mediaProviderName;
    }
}
