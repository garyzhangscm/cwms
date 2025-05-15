package com.garyzhangscm.cwms.adminserver.model.wms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

public class Carton implements Serializable {

        private Long id;

        private String name;

    private Long warehouseId;
    private Warehouse warehouse;

    private Double length;

    private Double width;
    private Double height;
    private Double fillRate;

    private Boolean enabled;

    private Boolean shippingCartonFlag;

    private Boolean pickingCartonFlag;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Double getTotalSpace() {
        return length * width * height * fillRate / 100;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
