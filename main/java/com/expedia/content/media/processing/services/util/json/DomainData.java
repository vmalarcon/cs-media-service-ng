
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
    "domainDataName",
    "domainDataFields"
})
public class DomainData {

    @JsonProperty("domainDataName")
    private String domainDataName;
    @JsonProperty("domainDataFields")
    private DomainDataFields domainDataFields;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * @return
     *     The domainDataName
     */
    @JsonProperty("domainDataName")
    public String getDomainDataName() {
        return domainDataName;
    }

    /**
     * 
     * @param domainDataName
     *     The domainDataName
     */
    @JsonProperty("domainDataName")
    public void setDomainDataName(String domainDataName) {
        this.domainDataName = domainDataName;
    }

    /**
     * 
     * @return
     *     The domainDataFields
     */
    @JsonProperty("domainDataFields")
    public DomainDataFields getDomainDataFields() {
        return domainDataFields;
    }

    /**
     * 
     * @param domainDataFields
     *     The domainDataFields
     */
    @JsonProperty("domainDataFields")
    public void setDomainDataFields(DomainDataFields domainDataFields) {
        this.domainDataFields = domainDataFields;
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
