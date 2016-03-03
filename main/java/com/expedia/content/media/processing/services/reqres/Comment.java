package com.expedia.content.media.processing.services.reqres;

/**
 * Comment Object with note and timestamp
 */
public class Comment {
    private final String note;
    private final String timestamp;

    public Comment(String note, String timestamp) {
        this.note = note;
        this.timestamp = timestamp;
    }

    public String getNote() {
        return note;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
