package com.expedia.content.media.processing.services.util;

/**
 * Media Service URL.
 */
public enum MediaServiceUrl {
    ACQUIRE_MEDIA("/acquireMedia"),
    MEDIA_IMAGES("/media/v1/images"),
    MEDIA_BY_DOMAIN("/media/v1/imagesbydomain"),
    MEDIA_STATUS("/media/v1/lateststatus"),
    MEDIA_DOMAIN_CATEGORIES("/media/v1/domaincategories"),
    MEDIA_TEMP_DERIVATIVE("/media/v1/tempderivative"),
    MEDIA_SOURCEURL("/media/v1/sourceurl"),
    MEDIA_SOURCEIMAGE("/media/v1/sourceimage"),
    MEDIA_DOWLOAD("/media/s3/download");

    private String url;

    private MediaServiceUrl(final String imageType) {
        this.url = imageType;
    }
    
    public String getUrl() {
        return this.url;
    }

}
