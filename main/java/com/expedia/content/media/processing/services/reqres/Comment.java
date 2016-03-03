package com.expedia.content.media.processing.services.reqres;

/**
 * Created by sstannus on 3/3/16.
 */
public class Comment {
    private final String note;
    private final String timestamp;

    public Comment(String note, String timestamp) {
        this.note = note;
        this.timestamp = timestamp;
    }
}
