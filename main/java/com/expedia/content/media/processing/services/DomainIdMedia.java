package com.expedia.content.media.processing.services;

import java.util.List;
import java.util.Map;

/**
 * Media response object representation.
 */
public class DomainIdMedia {

    private String mediaGuid;
    private String fileUrl;
    private String fileName;
    private String active;
    private Integer width;
    private Integer height;
    private Long fileSize;
    private String status;
    private String lastUpdatedBy;
    private String lastUpdateDateTime;
    private String domainProvider;
    private String domainDerivativCategory;
    private Map<String, Object> domainFields;
    private List<Map<String, Object>> derivatives;
    private List<String> comments;

    public String getMediaGuid() {
        return mediaGuid;
    }

    public void setMediaGuid(String mediaGuid) {
        this.mediaGuid = mediaGuid;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public String getLastUpdateDateTime() {
        return lastUpdateDateTime;
    }

    public void setLastUpdateDateTime(String lastUpdateDateTime) {
        this.lastUpdateDateTime = lastUpdateDateTime;
    }

    public String getDomainProvider() {
        return domainProvider;
    }

    public void setDomainProvider(String domainProvider) {
        this.domainProvider = domainProvider;
    }

    public String getDomainDerivativCategory() {
        return domainDerivativCategory;
    }

    public void setDomainDerivativCategory(String domainDerivativCategory) {
        this.domainDerivativCategory = domainDerivativCategory;
    }

    public Map<String, Object> getDomainFields() {
        return domainFields;
    }

    public void setDomainFields(Map<String, Object> domainFields) {
        this.domainFields = domainFields;
    }

    public List<Map<String, Object>> getDerivatives() {
        return derivatives;
    }

    public void setDerivatives(List<Map<String, Object>> derivatives) {
        this.derivatives = derivatives;
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }

}
