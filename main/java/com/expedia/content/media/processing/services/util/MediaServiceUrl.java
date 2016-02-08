package com.expedia.content.media.processing.services.util;

/**
 * Media Service URL.
 */
public enum MediaServiceUrl {
    ACQUIRE_MEDIA("/acquireMedia"),
    MEDIA_ADD("/media/v1/images"),
    MEDIA_BY_DOMAIN("/media/v1/images"),
    MEDIA_STATUS("/media/v1/lateststatus"),
    MEDIA_DOMAIN_CATEGORIES("/media/v1/domaincategories/");

    private String url;

    private MediaServiceUrl(final String imageType) {
        this.url = imageType;
    }
    
    public String getUrl() {
        return this.url;
    }

}
