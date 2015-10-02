package com.expedia.www.cs.media.controller;

import java.util.Date;

public class HelloMessage {

    private String message;

    private final String date = new Date().toString();

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }
}
