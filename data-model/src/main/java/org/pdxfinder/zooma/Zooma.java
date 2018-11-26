package org.pdxfinder.zooma;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "biologicalEntities",
        "property",
        "semanticTag",
        "provenance"
})
public class Zooma {

    @JsonProperty("biologicalEntities")
    private BiologicalEntities biologicalEntities;
    @JsonProperty("property")
    private Property property;
    @JsonProperty("semanticTag")
    private List<String> semanticTag = null;
    @JsonProperty("provenance")
    private Provenance provenance;

    @JsonProperty("biologicalEntities")
    public BiologicalEntities getBiologicalEntities() {
        return biologicalEntities;
    }

    @JsonProperty("biologicalEntities")
    public void setBiologicalEntities(BiologicalEntities biologicalEntities) {
        this.biologicalEntities = biologicalEntities;
    }

    @JsonProperty("property")
    public Property getProperty() {
        return property;
    }

    @JsonProperty("property")
    public void setProperty(Property property) {
        this.property = property;
    }

    @JsonProperty("semanticTag")
    public List<String> getSemanticTag() {
        return semanticTag;
    }

    @JsonProperty("semanticTag")
    public void setSemanticTag(List<String> semanticTag) {
        this.semanticTag = semanticTag;
    }

    @JsonProperty("provenance")
    public Provenance getProvenance() {
        return provenance;
    }

    @JsonProperty("provenance")
    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
    }

}