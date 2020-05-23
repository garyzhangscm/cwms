package com.garyzhangscm.cwms.outbound.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;


public class CartonCSVWrapper implements Serializable {


        private String name;

    private String warehouse;

    private Double length;

    private Double width;

    private Double height;

    private Double fillRate;

    private Boolean enabled;


    private Boolean shippingCartonFlag;

    private Boolean pickingCartonFlag;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getFillRate() {
        return fillRate;
    }

    public void setFillRate(Double fillRate) {
        this.fillRate = fillRate;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getShippingCartonFlag() {
        return shippingCartonFlag;
    }

    public void setShippingCartonFlag(Boolean shippingCartonFlag) {
        this.shippingCartonFlag = shippingCartonFlag;
    }

    public Boolean getPickingCartonFlag() {
        return pickingCartonFlag;
    }

    public void setPickingCartonFlag(Boolean pickingCartonFlag) {
        this.pickingCartonFlag = pickingCartonFlag;
    }
}
