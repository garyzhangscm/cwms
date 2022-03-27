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

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "location_group")
public class LocationGroup extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_group_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name="location_group_type_id")
    private LocationGroupType locationGroupType;

    @Column(name = "pickable")
    private Boolean pickable;
    @Column(name = "storable")
    private Boolean storable;
    @Column(name = "countable")
    private Boolean countable;
    @Column(name = "adjustable")
    private Boolean adjustable;
    @Column(name = "allow_cartonization")
    private Boolean allowCartonization = false;
    @Column(name = "tracking_volume")
    private Boolean trackingVolume;
    @Column(name = "allow_empty_location")
    private Boolean allowEmptyLocation = false;

    // Whether we tracking location's utilization
    // in this location groupd
    @Column(name = "tracking_location_utilization")
    @Enumerated(EnumType.STRING)
    private Boolean trackingLocationUtilization = false;

    // How we calculate the item's volume in the location
    // either by stock uom or by box
    @Column(name = "item_volume_tracking_level")
    @Enumerated(EnumType.STRING)
    private ItemVolumeTrackingLevel itemVolumeTrackingLevel;

    @Column(name = "volume_tracking_policy")
    @Enumerated(EnumType.STRING)
    private LocationVolumeTrackingPolicy volumeTrackingPolicy;


    @Column(name = "inventory_consolidation_strategy")
    @Enumerated(EnumType.STRING)
    private InventoryConsolidationStrategy inventoryConsolidationStrategy = InventoryConsolidationStrategy.NONE;


    @OneToMany(
            mappedBy = "locationGroup",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<PickableUnitOfMeasure> pickableUnitOfMeasures = new ArrayList<>();

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

    public Boolean getAdjustable() {
        return adjustable;
    }

    public void setAdjustable(Boolean adjustable) {
        this.adjustable = adjustable;
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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public InventoryConsolidationStrategy getInventoryConsolidationStrategy() {
        return inventoryConsolidationStrategy;
    }

    public void setInventoryConsolidationStrategy(InventoryConsolidationStrategy inventoryConsolidationStrategy) {
        this.inventoryConsolidationStrategy = inventoryConsolidationStrategy;
    }

    public Boolean getAllowCartonization() {
        return allowCartonization;
    }

    public void setAllowCartonization(Boolean allowCartonization) {
        this.allowCartonization = allowCartonization;
    }

    public List<PickableUnitOfMeasure> getPickableUnitOfMeasures() {
        return pickableUnitOfMeasures;
    }

    public void setPickableUnitOfMeasures(List<PickableUnitOfMeasure> pickableUnitOfMeasures) {
        this.pickableUnitOfMeasures = pickableUnitOfMeasures;
    }

    public Boolean getAllowEmptyLocation() {
        return allowEmptyLocation;
    }

    public void setAllowEmptyLocation(Boolean allowEmptyLocation) {
        this.allowEmptyLocation = allowEmptyLocation;
    }

    public ItemVolumeTrackingLevel getItemVolumeTrackingLevel() {
        return itemVolumeTrackingLevel;
    }

    public void setItemVolumeTrackingLevel(ItemVolumeTrackingLevel itemVolumeTrackingLevel) {
        this.itemVolumeTrackingLevel = itemVolumeTrackingLevel;
    }

    public Boolean getTrackingLocationUtilization() {
        return trackingLocationUtilization;
    }

    public void setTrackingLocationUtilization(Boolean trackingLocationUtilization) {
        this.trackingLocationUtilization = trackingLocationUtilization;
    }
}
