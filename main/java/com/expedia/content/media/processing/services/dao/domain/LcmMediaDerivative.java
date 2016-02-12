package com.expedia.content.media.processing.services.dao.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the media derivative from the MediaFileName table.S
 */
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

    private final Integer mediaId;
    private final Integer mediSizeTypeId;
    private final Boolean fileProcessed;
    private final String fileName;
    private final Integer width;
    private final Integer height;
    private final Integer fileSize;

    public LcmMediaDerivative(final Integer mediaId, final Integer mediSizeTypeId, final Boolean fileProcessed, final String fileName, final Integer width,
                           final Integer height, final Integer fileSize) {
        this.mediaId = mediaId;
        this.mediSizeTypeId = mediSizeTypeId;
        this.fileProcessed = fileProcessed;
        this.fileName = fileName;
        this.width = width;
        this.height = height;
        this.fileSize = fileSize;
    }

    public Integer getMediaId() {
        return mediaId;
    }

    public String getMediSizeType() {
        return TYPE_MAP.get(mediSizeTypeId);
    }

    public Boolean isFileProcessed() {
        return fileProcessed;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getFileSize() {
        return fileSize;
    }

}
