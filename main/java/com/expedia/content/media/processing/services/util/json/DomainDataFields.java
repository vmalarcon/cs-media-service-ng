
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
    "expediaId",
    "categoryId",
    "mediaProviderId",
    "propertyHero",
    "caption",
    "roomId",
    "roomHero"
})
public class DomainDataFields {

    @JsonProperty("expediaId")
    private String expediaId;
    @JsonProperty("categoryId")
    private String categoryId;
    @JsonProperty("mediaProviderId")
    private String mediaProviderId;
    @JsonProperty("propertyHero")
    private String propertyHero;
    @JsonProperty("caption")
    private String caption;
    @JsonProperty("roomId")
    private String roomId;
    @JsonProperty("roomHero")
    private String roomHero;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The expediaId
     */
    @JsonProperty("expediaId")
    public String getExpediaId() {
        return expediaId;
    }

    /**
     * 
     * @param expediaId
     *     The expediaId
     */
    @JsonProperty("expediaId")
    public void setExpediaId(String expediaId) {
        this.expediaId = expediaId;
    }

    /**
     * 
     * @return
     *     The categoryId
     */
    @JsonProperty("categoryId")
    public String getCategoryId() {
        return categoryId;
    }

    /**
     * 
     * @param categoryId
     *     The categoryId
     */
    @JsonProperty("categoryId")
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * 
     * @return
     *     The mediaProviderId
     */
    @JsonProperty("mediaProviderId")
    public String getMediaProviderId() {
        return mediaProviderId;
    }

    /**
     * 
     * @param mediaProviderId
     *     The mediaProviderId
     */
    @JsonProperty("mediaProviderId")
    public void setMediaProviderId(String mediaProviderId) {
        this.mediaProviderId = mediaProviderId;
    }

    /**
     * 
     * @return
     *     The propertyHero
     */
    @JsonProperty("propertyHero")
    public String getPropertyHero() {
        return propertyHero;
    }

    /**
     * 
     * @param propertyHero
     *     The propertyHero
     */
    @JsonProperty("propertyHero")
    public void setPropertyHero(String propertyHero) {
        this.propertyHero = propertyHero;
    }

    /**
     * 
     * @return
     *     The caption
     */
    @JsonProperty("caption")
    public String getCaption() {
        return caption;
    }

    /**
     * 
     * @param caption
     *     The caption
     */
    @JsonProperty("caption")
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * 
     * @return
     *     The roomId
     */
    @JsonProperty("roomId")
    public String getRoomId() {
        return roomId;
    }

    /**
     * 
     * @param roomId
     *     The roomId
     */
    @JsonProperty("roomId")
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    /**
     * 
     * @return
     *     The roomHero
     */
    @JsonProperty("roomHero")
    public String getRoomHero() {
        return roomHero;
    }

    /**
     * 
     * @param roomHero
     *     The roomHero
     */
    @JsonProperty("roomHero")
    public void setRoomHero(String roomHero) {
        this.roomHero = roomHero;
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
