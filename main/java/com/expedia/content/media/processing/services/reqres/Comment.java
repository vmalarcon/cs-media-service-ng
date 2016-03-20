package com.expedia.content.media.processing.services.reqres;

import lombok.Builder;
import lombok.Getter;

/**
 * Comment Object with note and timestamp
 */
@Builder
@Getter
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class Comment {
    private final String note;
    private final String timestamp;
}
