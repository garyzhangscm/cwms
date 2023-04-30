package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ShipmentRequestOrderVolumeParameters {
    @JsonProperty("box_no")
    private String boxNo;
    @JsonProperty("child_no")
    private String childNo;
    @JsonProperty("volume_height")
    private Double volumeHeight;
    @JsonProperty("volume_length")
    private Double volumeLength;
    @JsonProperty("volume_width")
    private Double volumeWidth;
    @JsonProperty("volume_weight")
    private Double volumeWeight;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getBoxNo() {
        return boxNo;
    }

    public void setBoxNo(String boxNo) {
        this.boxNo = boxNo;
    }

    public String getChildNo() {
        return childNo;
    }

    public void setChildNo(String childNo) {
        this.childNo = childNo;
    }

    public Double getVolumeHeight() {
        return volumeHeight;
    }

    public void setVolumeHeight(Double volumeHeight) {
        this.volumeHeight = volumeHeight;
    }

    public Double getVolumeLength() {
        return volumeLength;
    }

    public void setVolumeLength(Double volumeLength) {
        this.volumeLength = volumeLength;
    }

    public Double getVolumeWidth() {
        return volumeWidth;
    }

    public void setVolumeWidth(Double volumeWidth) {
        this.volumeWidth = volumeWidth;
    }

    public Double getVolumeWeight() {
        return volumeWeight;
    }

    public void setVolumeWeight(Double volumeWeight) {
        this.volumeWeight = volumeWeight;
    }
}
