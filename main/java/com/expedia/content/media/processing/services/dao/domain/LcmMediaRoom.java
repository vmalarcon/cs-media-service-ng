package com.expedia.content.media.processing.services.dao.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * Represents a room associated to a media from the CatalogItemMedia and RoomType tables.
 */
@Builder
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class LcmMediaRoom {

    @Getter private final int mediaId;
    @Getter private final int roomId;
    @Getter private final Boolean roomHero;

}
