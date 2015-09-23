package com.expedia.content.media.processing.services.util;

/**
 * Media Service URL.
 */
public enum MediaServiceUrl {
    ACQUIREMEDIA("/acquireMedia"),
    MEDIASTATUS("/media/v1/lateststatus");

    private String url;

    private MediaServiceUrl(final String imageType) {
        this.url = imageType;
    }
    
    public String getUrl() {
        return this.url;
    }

}
