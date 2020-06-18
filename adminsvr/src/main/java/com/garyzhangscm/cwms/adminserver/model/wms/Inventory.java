/**
 * Copyright 2018
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Inventory implements Serializable {

    private Long id;

    private String lpn;

    private Long locationId;
    private Location location;

    private Long pickId;

    private Long receiptId;

    private Item item;

    private ItemPackageType itemPackageType;

    private Long quantity;

    private Boolean virtual;

    private InventoryStatus inventoryStatus;

    private Long warehouseId;

    private Boolean lockedForAdjust = false;

    List<InventoryMovement> inventoryMovements = new ArrayList<>();


    public Inventory(){}
    public Inventory(String lpn,
                     Warehouse warehouse,
                     Location location,
                     Item item,
                     ItemPackageType itemPackageType,
                     InventoryStatus inventoryStatus,
                     Long quantity) {
        this.lpn = lpn;

        if (Objects.nonNull(location)) {

            this.locationId = location.getId();
        }
        this.location = location;

        this.pickId = null;

        this.receiptId = null;

        this.item = item;

        this.itemPackageType = itemPackageType;

        this.quantity = quantity;

        this.virtual = false;

        this.inventoryStatus = inventoryStatus;

        this.warehouseId = warehouse.getId();

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

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Long getPickId() {
        return pickId;
    }

    public void setPickId(Long pickId) {
        this.pickId = pickId;
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Long receiptId) {
        this.receiptId = receiptId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemPackageType getItemPackageType() {
        return itemPackageType;
    }

    public void setItemPackageType(ItemPackageType itemPackageType) {
        this.itemPackageType = itemPackageType;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Boolean getVirtual() {
        return virtual;
    }

    public void setVirtual(Boolean virtual) {
        this.virtual = virtual;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Boolean getLockedForAdjust() {
        return lockedForAdjust;
    }

    public void setLockedForAdjust(Boolean lockedForAdjust) {
        this.lockedForAdjust = lockedForAdjust;
    }

    public List<InventoryMovement> getInventoryMovements() {
        return inventoryMovements;
    }

    public void setInventoryMovements(List<InventoryMovement> inventoryMovements) {
        this.inventoryMovements = inventoryMovements;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
