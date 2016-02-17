package com.expedia.content.media.processing.services.dao.domain;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Represents the media data from the the Media and CatalogItemMedia tables.
 */
public class LcmMedia {

    private final Integer domainId;
    private final Integer mediaId;
    private final String fileName;
    private final Boolean active;
    private final Integer width;
    private final Integer height;
    private final Integer fileSize;
    private final String lastUpdatedBy;
    private final Date lastUpdateDate;
    private final Integer provider;
    private final Integer category;
    private final String comment;

    private List<LcmMediaDerivative> derivatives = Collections.EMPTY_LIST;
    private List<LcmMediaRoom> rooms = Collections.EMPTY_LIST;

    public LcmMedia(final Integer domainId, final Integer mediaId, final String fileName, final Boolean active, final Integer width, final Integer height,
                 final Integer fileSize, final String lastUpdatedBy, final Date lastUpdateDate, final Integer provider, final Integer category,
                 final String comment) {
        this.domainId = domainId;
        this.mediaId = mediaId;
        this.fileName = fileName;
        this.active = active;
        this.width = width;
        this.height = height;
        this.fileSize = fileSize;
        this.lastUpdatedBy = lastUpdatedBy;
        this.lastUpdateDate = lastUpdateDate;
        this.provider = provider;
        this.category = category;
        this.comment = comment;
    }

    public Integer getDomainId() {
        return domainId;
    }

    public Integer getMediaId() {
        return mediaId;
    }

    public String getFileName() {
        return fileName;
    }

    public Boolean isActive() {
        return active;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public Integer getProvider() {
        return provider;
    }

    public Integer getCategory() {
        return category;
    }

    public String getComment() {
        return comment;
    }

    public List<LcmMediaDerivative> getDerivatives() {
        return derivatives;
    }

    public void setDerivatives(List<LcmMediaDerivative> derivatives) {
        this.derivatives = derivatives;
    }

    public List<LcmMediaRoom> getRooms() {
        return rooms;
    }

    public void setRooms(List<LcmMediaRoom> rooms) {
        this.rooms = rooms;
    }

}
