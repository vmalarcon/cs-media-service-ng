package com.expedia.content.media.processing.services.dao.domain;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class Thumbnail {
    private final String size;
    private final String height;
    private final String widht;
    private final String location;
    private final String type;
}
