package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ShipmentResponseChild {
    @JsonProperty("child_number")
    private String childNumber;
    @JsonProperty("label_info")
    private String labelInfo;


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getChildNumber() {
        return childNumber;
    }

    public void setChildNumber(String childNumber) {
        this.childNumber = childNumber;
    }

    public String getLabelInfo() {
        return labelInfo;
    }

    public void setLabelInfo(String labelInfo) {
        this.labelInfo = labelInfo;
    }
}
