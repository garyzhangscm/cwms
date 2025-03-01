package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


public class ProductionLine {

    private Long id;

    private String name;

    private Long warehouseId;

    private Warehouse warehouse;

    private Long inboundStageLocationId;

    private Location inboundStageLocation;

    private Long outboundStageLocationId;

    private Location outboundStageLocation;

    private Long productionLineLocationId;

    private Location productionLineLocation;


    @Override
    public String toString(){
        return new StringBuilder()
                .append("id: ").append(id).append("\n")
                .append("name: ").append(name).append("\n")
                .append("warehouseId: ").append(warehouseId).append("\n")
                .append("warehouse: ").append(warehouse).append("\n")
                .append("inboundStageLocationId: ").append(inboundStageLocationId).append("\n")
                .append("inboundStageLocation: ").append(inboundStageLocation).append("\n")
                .append("outboundStageLocationId: ").append(outboundStageLocationId).append("\n")
                .append("outboundStageLocation: ").append(outboundStageLocation).append("\n")
                .append("productionLineLocationId: ").append(productionLineLocationId).append("\n")
                .append("productionLineLocation: ").append(productionLineLocation).append("\n")
                .toString();
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getInboundStageLocationId() {
        return inboundStageLocationId;
    }

    public void setInboundStageLocationId(Long inboundStageLocationId) {
        this.inboundStageLocationId = inboundStageLocationId;
    }

    public Location getInboundStageLocation() {
        return inboundStageLocation;
    }

    public void setInboundStageLocation(Location inboundStageLocation) {
        this.inboundStageLocation = inboundStageLocation;
    }

    public Long getOutboundStageLocationId() {
        return outboundStageLocationId;
    }

    public void setOutboundStageLocationId(Long outboundStageLocationId) {
        this.outboundStageLocationId = outboundStageLocationId;
    }

    public Location getOutboundStageLocation() {
        return outboundStageLocation;
    }

    public void setOutboundStageLocation(Location outboundStageLocation) {
        this.outboundStageLocation = outboundStageLocation;
    }

    public Long getProductionLineLocationId() {
        return productionLineLocationId;
    }

    public void setProductionLineLocationId(Long productionLineLocationId) {
        this.productionLineLocationId = productionLineLocationId;
    }

    public Location getProductionLineLocation() {
        return productionLineLocation;
    }

    public void setProductionLineLocation(Location productionLineLocation) {
        this.productionLineLocation = productionLineLocation;
    }


}
