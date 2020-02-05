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

package com.garyzhangscm.cwms.workorder.model;


import javax.persistence.Column;

public class LocationGroupType {

    private Long id;

    private String name;

    private String description;


    private Boolean fourWallInventory;

    private Boolean virtual;

    private Boolean receivingStage;
    private Boolean shippingStage;
    private Boolean dock;
    private Boolean yard;
    private Boolean storage;
    private Boolean pickupAndDeposit;

    private Boolean productionLine;
    private Boolean productionLineInbound;
    private Boolean productionLineOutounbd;

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

    public Boolean getProductionLineOutounbd() {
        return productionLineOutounbd;
    }

    public void setProductionLineOutounbd(Boolean productionLineOutounbd) {
        this.productionLineOutounbd = productionLineOutounbd;
    }
}
