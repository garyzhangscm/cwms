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

public class LocationGroupCSVWrapper {


    private String company;
    private String name;

    private String description;

    private String locationGroupType;

    private Boolean pickable;
    private Boolean storable;
    private Boolean countable;
    private Boolean adjustable;
    private Boolean allowCartonization;
    private Boolean trackingVolume;
    private Boolean shippedInventory;

    private String volumeTrackingPolicy;
    private String inventoryConsolidationStrategy;

    private String warehouse;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
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

    public String getLocationGroupType() {
        return locationGroupType;
    }

    public void setLocationGroupType(String locationGroupType) {
        this.locationGroupType = locationGroupType;
    }

    public Boolean getPickable() {
        return pickable;
    }

    public void setPickable(Boolean pickable) {
        this.pickable = pickable;
    }

    public Boolean getStorable() {
        return storable;
    }

    public void setStorable(Boolean storable) {
        this.storable = storable;
    }

    public Boolean getCountable() {
        return countable;
    }

    public void setCountable(Boolean countable) {
        this.countable = countable;
    }

    public Boolean getTrackingVolume() {
        return trackingVolume;
    }

    public void setTrackingVolume(Boolean trackingVolume) {
        this.trackingVolume = trackingVolume;
    }

    public Boolean getAdjustable() {
        return adjustable;
    }

    public void setAdjustable(Boolean adjustable) {
        this.adjustable = adjustable;
    }

    public String getVolumeTrackingPolicy() {
        return volumeTrackingPolicy;
    }

    public void setVolumeTrackingPolicy(String volumeTrackingPolicy) {
        this.volumeTrackingPolicy = volumeTrackingPolicy;
    }

    public Boolean getShippedInventory() {
        return shippedInventory;
    }

    public void setShippedInventory(Boolean shippedInventory) {
        this.shippedInventory = shippedInventory;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getInventoryConsolidationStrategy() {
        return inventoryConsolidationStrategy;
    }

    public void setInventoryConsolidationStrategy(String inventoryConsolidationStrategy) {
        this.inventoryConsolidationStrategy = inventoryConsolidationStrategy;
    }

    public Boolean getAllowCartonization() {
        return allowCartonization;
    }

    public void setAllowCartonization(Boolean allowCartonization) {
        this.allowCartonization = allowCartonization;
    }

}
