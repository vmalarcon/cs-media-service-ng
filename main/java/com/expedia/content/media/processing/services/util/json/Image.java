
package com.expedia.content.media.processing.services.util.json;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "fileUrl",
    "fileName",
    "imageType",
    "domainData"
})
public class Image {

    @JsonProperty("fileUrl")
    private String fileUrl;
    @JsonProperty("fileName")
    private String fileName;
    @JsonProperty("imageType")
    private String imageType;
    @JsonProperty("domainData")
    private DomainData domainData;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The fileUrl
     */
    @JsonProperty("fileUrl")
    public String getFileUrl() {
        return fileUrl;
    }

    /**
     * 
     * @param fileUrl
     *     The fileUrl
     */
    @JsonProperty("fileUrl")
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    /**
     * 
     * @return
     *     The fileName
     */
    @JsonProperty("fileName")
    public String getFileName() {
        return fileName;
    }

    /**
     * 
     * @param fileName
     *     The fileName
     */
    @JsonProperty("fileName")
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * 
     * @return
     *     The imageType
     */
    @JsonProperty("imageType")
    public String getImageType() {
        return imageType;
    }

    /**
     * 
     * @param imageType
     *     The imageType
     */
    @JsonProperty("imageType")
    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    /**
     * 
     * @return
     *     The domainData
     */
    @JsonProperty("domainData")
    public DomainData getDomainData() {
        return domainData;
    }

    /**
     * 
     * @param domainData
     *     The domainData
     */
    @JsonProperty("domainData")
    public void setDomainData(DomainData domainData) {
        this.domainData = domainData;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
