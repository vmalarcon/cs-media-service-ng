package com.expedia.content.media.processing.services.dao;

/**
 * Represents the data retrieved from LCM of a Media.
 */
public final class CatalogItemMedia {

    private int catalogItemId;
    private int mediaID;
    private int mediaUseRank;
    private boolean galleryDisplayBool;
    private int mediaUseTypeID;
    private int updateTravelProductID;
    private int updateTUID ;
    private boolean pictureShowDisplayBool;
    private boolean paragraphDisplayBool;
    private String lastUpdatedBy;
    private String updateLocation;

    public CatalogItemMedia(int catalogItemId, int mediaID, int mediaUseRank, boolean galleryDisplayBool,
                            int mediaUseTypeID, int updateTravelProductID, int updateTUID, boolean pictureShowDisplayBool,
                            boolean paragraphDisplayBool, String lastUpdatedBy, String updateLocation) {
        this.catalogItemId = catalogItemId;
        this.mediaID = mediaID;
        this.mediaUseRank = mediaUseRank;
        this.galleryDisplayBool = galleryDisplayBool;
        this.mediaUseTypeID = mediaUseTypeID;
        this.updateTravelProductID = updateTravelProductID;
        this.updateTUID  = updateTUID;
        this.pictureShowDisplayBool = pictureShowDisplayBool;
        this.paragraphDisplayBool = paragraphDisplayBool;
        this.lastUpdatedBy = lastUpdatedBy;
        this.updateLocation = updateLocation;
    }

    public int getMediaID() {
        return mediaID;
    }

    public int getCatalogItemId() {
        return catalogItemId;
    }

    public int getMediaUseRank() {
        return mediaUseRank;
    }

    public boolean getGalleryDisplayBool() {
        return galleryDisplayBool;
    }

    public int getMediaUseTypeID() {
        return mediaUseTypeID;
    }

    public int getUpdateTravelProductID() {
        return updateTravelProductID;
    }

    public int getUpdateTUID() {
        return updateTUID;
    }

    public boolean getPictureShowDisplayBool() {
        return pictureShowDisplayBool;
    }

    public boolean getParagraphDisplayBool() {
        return paragraphDisplayBool;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public String getUpdateLocation() {
        return updateLocation;
    }

    @Override
    public boolean equals(Object obj){
        final CatalogItemMedia other = (CatalogItemMedia) obj;
        return (this.catalogItemId == other.getCatalogItemId() && this.mediaID == other.getMediaID() && this.mediaUseRank == other.getMediaUseRank() &&
                this.galleryDisplayBool == other.getGalleryDisplayBool() && this.mediaUseTypeID == other.getMediaUseTypeID() &&
                this.updateTravelProductID == other.getUpdateTravelProductID() && this.updateTUID == other.getUpdateTUID() &&
                this.pictureShowDisplayBool == other.getPictureShowDisplayBool() && this.paragraphDisplayBool == other.getParagraphDisplayBool()
                && this.lastUpdatedBy.equals(other.getLastUpdatedBy()) && this.updateLocation.equals(other.getUpdateLocation()));

    }
}
