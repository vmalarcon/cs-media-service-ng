package com.expedia.content.media.processing.services;

import java.util.List;

/**
 * Media By domain id message response.
 */
public class MediaByDomainIdResponse {

    private String domain;
    private String domainId;
    private List<DomainIdMedia> images;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public List<DomainIdMedia> getImages() {
        return images;
    }

    public void setImages(List<DomainIdMedia> images) {
        this.images = images;
    }

}
