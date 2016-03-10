package com.expedia.content.media.processing.services.reqres;

/**
 * Represents a Temporary Derivative Message sent to media/v1/tempderivative
 */
public class TempDerivativeMessage {
    private String fileUrl;
    private String rotation;
    private Integer width;
    private Integer height;

    public TempDerivativeMessage(String fileUrl, String rotation, Integer width, Integer height) {
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

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
