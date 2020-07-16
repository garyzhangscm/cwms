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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AllocationConfiguration implements Serializable {

    private Long id;

    private Integer sequence;

    // criteria: item / item group / inventory status
    private Long itemId;

    private Item item;

    private Long warehouseId;

    private Warehouse warehouse;

    private Long itemFamilyId;

    private ItemFamily itemFamily;

    private AllocationConfigurationType type;

    private Long locationId;

    private Location location;

    private Long locationGroupId;
    private LocationGroup locationGroup;

    private Long locationGroupTypeId;

    private LocationGroupType locationGroupType;

    private AllocationStrategy allocationStrategy;

    private List<PickableUnitOfMeasure> pickableUnitOfMeasures = new ArrayList<>();

    public AllocationConfiguration() {}


    public AllocationConfiguration(Integer sequence,
                                   Warehouse warehouse,
                                   ItemFamily itemFamily,
                                   AllocationConfigurationType type,
                                   LocationGroup locationGroup,
                                   AllocationStrategy allocationStrategy,
                                   List<PickableUnitOfMeasure> pickableUnitOfMeasures) {
        this.sequence = sequence;

        this.warehouseId = warehouse.getId();
        this.warehouse = warehouse;

        this.itemFamilyId = itemFamily.getId();
        this.itemFamily = itemFamily;

        this.type = type;

        this.locationGroupId = locationGroup.getId();
        this.locationGroup = locationGroup;

        this.allocationStrategy = allocationStrategy;
        this.pickableUnitOfMeasures = pickableUnitOfMeasures;
    }
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

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Long getItemFamilyId() {
        return itemFamilyId;
    }

    public void setItemFamilyId(Long itemFamilyId) {
        this.itemFamilyId = itemFamilyId;
    }

    public ItemFamily getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(ItemFamily itemFamily) {
        this.itemFamily = itemFamily;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Long getLocationGroupId() {
        return locationGroupId;
    }

    public void setLocationGroupId(Long locationGroupId) {
        this.locationGroupId = locationGroupId;
    }

    public LocationGroup getLocationGroup() {
        return locationGroup;
    }

    public void setLocationGroup(LocationGroup locationGroup) {
        this.locationGroup = locationGroup;
    }

    public Long getLocationGroupTypeId() {
        return locationGroupTypeId;
    }

    public void setLocationGroupTypeId(Long locationGroupTypeId) {
        this.locationGroupTypeId = locationGroupTypeId;
    }

    public LocationGroupType getLocationGroupType() {
        return locationGroupType;
    }

    public void setLocationGroupType(LocationGroupType locationGroupType) {
        this.locationGroupType = locationGroupType;
    }

    public AllocationStrategy getAllocationStrategy() {
        return allocationStrategy;
    }

    public void setAllocationStrategy(AllocationStrategy allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
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

    public AllocationConfigurationType getType() {
        return type;
    }

    public void setType(AllocationConfigurationType type) {
        this.type = type;
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
}
