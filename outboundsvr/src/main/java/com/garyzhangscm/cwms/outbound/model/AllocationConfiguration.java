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

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "allocation_configuration")
public class AllocationConfiguration  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "sequence")
    private Integer sequence;

    // criteria: item / item group / inventory status

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;


    @Column(name = "item_family_id")
    private Long itemFamilyId;

    @Transient
    private ItemFamily itemFamily;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private AllocationConfigurationType type;

    // source
    @Column(name = "location_id")
    private Long locationId;

    @Transient
    private Location location;

    @Column(name = "location_group_id")
    private Long locationGroupId;
    @Transient
    private LocationGroup locationGroup;

    @Column(name = "location_group_type_id")
    private Long locationGroupTypeId;

    @Transient
    private LocationGroupType locationGroupType;

    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;

    @Transient
    private InventoryStatus inventoryStatus;

    @OneToMany(
            mappedBy = "allocationConfiguration",
            orphanRemoval = true,
            cascade = CascadeType.ALL
    )
    private List<AllocationConfigurationPickableUnitOfMeasure> allocationConfigurationPickableUnitOfMeasures = new ArrayList<>();

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


    public List<AllocationConfigurationPickableUnitOfMeasure> getAllocationConfigurationPickableUnitOfMeasures() {
        return allocationConfigurationPickableUnitOfMeasures;
    }

    public void setAllocationConfigurationPickableUnitOfMeasures(List<AllocationConfigurationPickableUnitOfMeasure> allocationConfigurationPickableUnitOfMeasures) {
        this.allocationConfigurationPickableUnitOfMeasures = allocationConfigurationPickableUnitOfMeasures;
    }
    public void addPickableUnitOfMeasure(AllocationConfigurationPickableUnitOfMeasure allocationConfigurationPickableUnitOfMeasure) {
        this.allocationConfigurationPickableUnitOfMeasures.add(allocationConfigurationPickableUnitOfMeasure);
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

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }
}
