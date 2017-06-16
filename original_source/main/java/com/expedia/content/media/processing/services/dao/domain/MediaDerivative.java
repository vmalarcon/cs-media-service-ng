package com.expedia.content.media.processing.services.dao.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Object to represent a derivative record in Media DB.
 */
@AllArgsConstructor
@Builder
@Getter
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField", "PMD.ImmutableField"})
public class MediaDerivative {
    private String mediaGuid;
    private String location;
    private String type;
    private Integer width;
    private Integer height;
    private Integer fileSize;

}
