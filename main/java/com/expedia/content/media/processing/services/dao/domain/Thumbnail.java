package com.expedia.content.media.processing.services.dao.domain;

import com.expedia.content.media.processing.pipeline.domain.Metadata;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class Thumbnail {
    private Metadata thumbnailMetadata;
    private final String location;
    private final String type;
    private Metadata sourceMetadata;
}
