package com.expedia.content.media.processing.services.dao.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * Represents the media derivative from the MediaFileName table.
 */
@Builder
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class LcmMediaDerivative {

    @SuppressWarnings({"serial", "PMD.NonStaticInitializer"})
    private static final Map<Integer, String> TYPE_MAP = Collections.unmodifiableMap(new HashMap<Integer, String>() {
        {
            put(1, "t");
            put(2, "s");
            put(3, "b");
            put(4, "c");
            put(5, "x");
            put(6, "f");
            put(7, "p");
            put(8, "v");
            put(9, "l");
            put(10, "m");
            put(11, "n");
            put(12, "g");
            put(13, "d");
            put(14, "y");
            put(15, "z");
            put(16, "e");
            put(17, "w");
        }
    });

    @Getter private final Integer mediaId;
    private final Integer mediaSizeTypeId;
    @Getter private final Boolean fileProcessed;
    @Getter private final String fileName;
    @Getter private final Integer width;
    @Getter private final Integer height;
    @Getter private final Integer fileSize;

    public String getMediaSizeType() {
        return TYPE_MAP.get(mediaSizeTypeId);
    }

}
