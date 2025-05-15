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

package com.garyzhangscm.cwms.inventory.model;

import java.util.Objects;

public class AllocationDryRunResult {


    private Long warehouseId;

    // requested Item and inventory status
    private Item item;
    private InventoryStatus inventoryStatus;

    // result of the dry run on certain inventory
    private Inventory inventory;

    private String locationName;

    private Long locationInventoryQuantity;
    private Long locationOpenPickQuantity;

    private boolean allocatible;

    // only if the inventory is not allocatible based on the
    // requirement
    private String allocationFailReason;

    public AllocationDryRunResult() {}

    public AllocationDryRunResult(String locationName, Inventory inventory,
                                  Long locationInventoryQuantity, Long locationOpenPickQuantity) {
        this.locationName = locationName;
        this.warehouseId = inventory.getWarehouseId();
        this.item = inventory.getItem();
        this.inventoryStatus = inventory.getInventoryStatus();
        this.inventory = inventory;
        this.allocatible = false;
        this.allocationFailReason = "";
        this.locationInventoryQuantity = locationInventoryQuantity;
        this.locationOpenPickQuantity = locationOpenPickQuantity;
    }

    public AllocationDryRunResult(Inventory inventory) {
        this("", inventory, 0l, 0l);
        if (Objects.nonNull(inventory.getLocation())) {
            setLocationName(inventory.getLocation().getName());
        }
    }

    public AllocationDryRunResult(Inventory inventory,
                                  Long locationInventoryQuantity, Long locationOpenPickQuantity) {
        this("", inventory, locationInventoryQuantity, locationOpenPickQuantity);
        if (Objects.nonNull(inventory.getLocation())) {
            setLocationName(inventory.getLocation().getName());
        }
    }

    public AllocationDryRunResult fail(String allocationFailReason) {
        setAllocationFailReason(allocationFailReason);
        setAllocatible(false);
        return this;
    }

    public AllocationDryRunResult succeed() {
        setAllocationFailReason("");
        setAllocatible(true);
        return this;
    }


    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getLocationInventoryQuantity() {
        return locationInventoryQuantity;
    }

    public void setLocationInventoryQuantity(Long locationInventoryQuantity) {
        this.locationInventoryQuantity = locationInventoryQuantity;
    }

    public Long getLocationOpenPickQuantity() {
        return locationOpenPickQuantity;
    }

    public void setLocationOpenPickQuantity(Long locationOpenPickQuantity) {
        this.locationOpenPickQuantity = locationOpenPickQuantity;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public boolean isAllocatible() {
        return allocatible;
    }

    public void setAllocatible(boolean allocatible) {
        this.allocatible = allocatible;
    }

    public String getAllocationFailReason() {
        return allocationFailReason;
    }

    public void setAllocationFailReason(String allocationFailReason) {
        this.allocationFailReason = allocationFailReason;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
