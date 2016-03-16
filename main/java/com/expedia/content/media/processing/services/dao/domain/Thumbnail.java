package com.expedia.content.media.processing.services.dao.domain;

import com.expedia.content.media.processing.pipeline.domain.Metadata;

import lombok.Builder;
import lombok.Getter;
/**
 * 
 * Represent a thumbnail object.
 *
 */
@Builder
@Getter
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class Thumbnail {
    private final Metadata thumbnailMetadata;
    private final String location;
    private final String type;
    private final Metadata sourceMetadata;
}
