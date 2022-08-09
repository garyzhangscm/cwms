/**
 * Copyright 2019
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.layout.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "location_group_type")
public class LocationGroupType extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_group_type_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;


    @Column(name = "description")
    private String description;


    @Column(name = "four_wall_inventory")
    private Boolean fourWallInventory;

    @Column(name = "virtual_locations")
    private Boolean virtual;

    @Column(name = "receiving_stage_locations")
    private Boolean receivingStage;
    @Column(name = "customer_return_stage_locations")
    private Boolean customerReturnStageLocation;
    @Column(name = "shipping_stage_locations")
    private Boolean shippingStage;
    @Column(name = "production_line_locations")
    private Boolean productionLine;
    @Column(name = "production_line_inbound_locations")
    private Boolean productionLineInbound;
    @Column(name = "production_line_outbound_locations")
    private Boolean productionLineOutbound;
    @Column(name = "dock_locations")
    private Boolean dock;
    @Column(name = "yard_locations")
    private Boolean yard;
    @Column(name = "storage_locations")
    private Boolean storage;
    @Column(name = "rf_locations")
    private Boolean rf;
    // a typical grid is a distribution wall
    @Column(name = "grid")
    private Boolean grid;
    @Column(name = "pickup_and_deposit_locations")
    private Boolean pickupAndDeposit;
    @Column(name = "trailer_locations")
    private Boolean trailer;
    @Column(name = "shipped_parcel_locations")
    private Boolean shippedParcel;
    @Column(name = "shipped_inventory_locations")
    private Boolean shippedInventory;
    @Column(name = "shipped_order_locations")
    private Boolean shippedOrder;
    @Column(name = "container_locations")
    private Boolean container;
    @Column(name = "packing_stations")
    private Boolean packingStation;
    @Column(name = "qc_area")
    private Boolean qcArea;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getFourWallInventory() {
        return fourWallInventory;
    }

    public void setFourWallInventory(Boolean fourWallInventory) {
        this.fourWallInventory = fourWallInventory;
    }

    public Boolean getVirtual() {
        return virtual;
    }

    public void setVirtual(Boolean virtual) {
        this.virtual = virtual;
    }

    public Boolean getReceivingStage() {
        return receivingStage;
    }

    public void setReceivingStage(Boolean receivingStage) {
        this.receivingStage = receivingStage;
    }

    public Boolean getShippingStage() {
        return shippingStage;
    }

    public void setShippingStage(Boolean shippingStage) {
        this.shippingStage = shippingStage;
    }

    public Boolean getDock() {
        return dock;
    }

    public void setDock(Boolean dock) {
        this.dock = dock;
    }

    public Boolean getRf() {
        return rf;
    }

    public void setRf(Boolean rf) {
        this.rf = rf;
    }

    public Boolean getYard() {
        return yard;
    }

    public void setYard(Boolean yard) {
        this.yard = yard;
    }

    public Boolean getStorage() {
        return storage;
    }

    public void setStorage(Boolean storage) {
        this.storage = storage;
    }

    public Boolean getPickupAndDeposit() {
        return pickupAndDeposit;
    }

    public void setPickupAndDeposit(Boolean pickupAndDeposit) {
        this.pickupAndDeposit = pickupAndDeposit;
    }

    public Boolean getTrailer() {
        return trailer;
    }

    public void setTrailer(Boolean trailer) {
        this.trailer = trailer;
    }

    public Boolean getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(Boolean productionLine) {
        this.productionLine = productionLine;
    }

    public Boolean getProductionLineInbound() {
        return productionLineInbound;
    }

    public void setProductionLineInbound(Boolean productionLineInbound) {
        this.productionLineInbound = productionLineInbound;
    }

    public Boolean getProductionLineOutbound() {
        return productionLineOutbound;
    }

    public void setProductionLineOutbound(Boolean productionLineOutbound) {
        this.productionLineOutbound = productionLineOutbound;
    }

    public Boolean getCustomerReturnStageLocation() {
        return customerReturnStageLocation;
    }

    public void setCustomerReturnStageLocation(Boolean customerReturnStageLocation) {
        this.customerReturnStageLocation = customerReturnStageLocation;
    }

    public Boolean getGrid() {
        return grid;
    }

    public void setGrid(Boolean grid) {
        this.grid = grid;
    }

    public Boolean getContainer() {
        return container;
    }

    public void setContainer(Boolean container) {
        this.container = container;
    }

    public Boolean getPackingStation() {
        return packingStation;
    }

    public void setPackingStation(Boolean packingStation) {
        this.packingStation = packingStation;
    }

    public Boolean getShippedParcel() {
        return shippedParcel;
    }

    public void setShippedParcel(Boolean shippedParcel) {
        this.shippedParcel = shippedParcel;
    }

    public Boolean getShippedOrder() {
        return shippedOrder;
    }

    public void setShippedOrder(Boolean shippedOrder) {
        this.shippedOrder = shippedOrder;
    }

    public Boolean getQcArea() {
        return qcArea;
    }

    public void setQcArea(Boolean qcArea) {
        this.qcArea = qcArea;
    }

    public Boolean getShippedInventory() {
        return shippedInventory;
    }

    public void setShippedInventory(Boolean shippedInventory) {
        this.shippedInventory = shippedInventory;
    }
}
