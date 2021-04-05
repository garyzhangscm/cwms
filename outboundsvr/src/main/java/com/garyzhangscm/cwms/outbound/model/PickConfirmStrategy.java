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

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pick_confirm_strategy")
public class PickConfirmStrategy extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pick_confirm_strategy_id")
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

    @Column(name = "unit_of_measure_id")
    private Long unitOfMeasureId;

    @Transient
    private UnitOfMeasure unitOfMeasure;

    // required field to confirm a pick
    @Column(name = "confirm_item_flag")
    private boolean confirmItemFlag;
    @Column(name = "confirm_location_flag")
    private boolean confirmLocationFlag;
    @Column(name = "confirm_location_code_flag")
    private boolean confirmLocationCodeFlag;


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

    public Long getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(Long unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public boolean isConfirmItemFlag() {
        return confirmItemFlag;
    }

    public void setConfirmItemFlag(boolean confirmItemFlag) {
        this.confirmItemFlag = confirmItemFlag;
    }

    public boolean isConfirmLocationFlag() {
        return confirmLocationFlag;
    }

    public void setConfirmLocationFlag(boolean confirmLocationFlag) {
        this.confirmLocationFlag = confirmLocationFlag;
    }

    public boolean isConfirmLocationCodeFlag() {
        return confirmLocationCodeFlag;
    }

    public void setConfirmLocationCodeFlag(boolean confirmLocationCodeFlag) {
        this.confirmLocationCodeFlag = confirmLocationCodeFlag;
    }
}
