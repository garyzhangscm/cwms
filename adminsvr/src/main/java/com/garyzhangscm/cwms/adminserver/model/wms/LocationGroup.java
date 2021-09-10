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

package com.garyzhangscm.cwms.adminserver.model.wms;


import java.util.ArrayList;
import java.util.List;

public class LocationGroup {

    private Long id;
    private String name;

    private String description;

    private Warehouse warehouse;

    private LocationGroupType locationGroupType;

    private Boolean pickable;
    private Boolean storable;
    private Boolean countable;
    private Boolean adjustable;
    private Boolean allowCartonization ;
    private Boolean trackingVolume;


    private LocationVolumeTrackingPolicy volumeTrackingPolicy;

    private InventoryConsolidationStrategy inventoryConsolidationStrategy = InventoryConsolidationStrategy.NONE;

    private List<PickableUnitOfMeasure> pickableUnitOfMeasures = new ArrayList<>();

    public LocationGroup() {}
    public LocationGroup(Warehouse warehouse, LocationGroupType locationGroupType, String name, String description) {
        this(warehouse, locationGroupType, name, description, InventoryConsolidationStrategy.NONE);
    }

    public LocationGroup(Warehouse warehouse, LocationGroupType locationGroupType,
                         String name, String description,
                         InventoryConsolidationStrategy inventoryConsolidationStrategy) {
        this.name = name;
        this.description = description;
        this.warehouse = warehouse;
        this.locationGroupType = locationGroupType;
        this.pickable = true;
        this.storable = true;
        this.countable = true;
        this.adjustable = true;
        this.allowCartonization = true;
        this.trackingVolume = true;


        this.volumeTrackingPolicy = LocationVolumeTrackingPolicy.BY_VOLUME;
        this.inventoryConsolidationStrategy = inventoryConsolidationStrategy;
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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public LocationGroupType getLocationGroupType() {
        return locationGroupType;
    }

    public void setLocationGroupType(LocationGroupType locationGroupType) {
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

    public Boolean getAllowCartonization() {
        return allowCartonization;
    }

    public void setAllowCartonization(Boolean allowCartonization) {
        this.allowCartonization = allowCartonization;
    }

    public Boolean getTrackingVolume() {
        return trackingVolume;
    }

    public void setTrackingVolume(Boolean trackingVolume) {
        this.trackingVolume = trackingVolume;
    }

    public LocationVolumeTrackingPolicy getVolumeTrackingPolicy() {
        return volumeTrackingPolicy;
    }

    public void setVolumeTrackingPolicy(LocationVolumeTrackingPolicy volumeTrackingPolicy) {
        this.volumeTrackingPolicy = volumeTrackingPolicy;
    }

    public InventoryConsolidationStrategy getInventoryConsolidationStrategy() {
        return inventoryConsolidationStrategy;
    }

    public void setInventoryConsolidationStrategy(InventoryConsolidationStrategy inventoryConsolidationStrategy) {
        this.inventoryConsolidationStrategy = inventoryConsolidationStrategy;
    }

    public Boolean getAdjustable() {
        return adjustable;
    }

    public void setAdjustable(Boolean adjustable) {
        this.adjustable = adjustable;
    }

    public List<PickableUnitOfMeasure> getPickableUnitOfMeasures() {
        return pickableUnitOfMeasures;
    }

    public void setPickableUnitOfMeasures(List<PickableUnitOfMeasure> pickableUnitOfMeasures) {
        this.pickableUnitOfMeasures = pickableUnitOfMeasures;
    }

    public void addPickableUnitOfMeasure(PickableUnitOfMeasure pickableUnitOfMeasure) {
        this.pickableUnitOfMeasures.add(pickableUnitOfMeasure);
    }
}
