package com.expedia.content.media.processing.services.derivative;

/**
 * Object model of Temporary Derivative Message
 */
public class TempDerivativeMessage {
    private String fileUrl;
    private String rotation;
    private int width;
    private int height;

    public TempDerivativeMessage(String fileUrl, String rotation, int width, int height) {
        this.fileUrl = fileUrl;
        this.rotation = rotation;
        this.width = width;
        this.height = height;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getRotation() {
        return rotation;
    }

    public void setRotation(String rotation) {
        this.rotation = rotation;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
