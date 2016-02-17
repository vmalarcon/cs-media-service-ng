package com.expedia.content.media.processing.services.dao.domain;

/**
 * Represents a room associated to a media from the CatalogItemMedia and RoomType tables.
 */
public class LcmMediaRoom {

    private final int roomId;

    public LcmMediaRoom(int roomId) {
        this.roomId = roomId;
    }

    public int getRoomId() {
        return roomId;
    }

}
