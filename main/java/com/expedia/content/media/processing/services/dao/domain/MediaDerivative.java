package com.expedia.content.media.processing.services.dao.domain;

import lombok.Getter;


public class MediaDerivative {
    @Getter private String mediaGuid;
    @Getter private String location;
    @Getter private String type;
    @Getter private Integer width;
    @Getter private Integer height;
    @Getter private Integer fileSize;

    public MediaDerivative(String mediaGuid, String location, String type, Integer width, Integer height, Integer fileSize) {
        this.mediaGuid = mediaGuid;
        this.location = location;
        this.type = type;
        this.width = width;
        this.height = height;
        this.fileSize = fileSize;
    }
}
