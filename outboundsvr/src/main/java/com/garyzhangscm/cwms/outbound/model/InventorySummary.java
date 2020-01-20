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

package com.garyzhangscm.cwms.outbound.model;

import java.io.Serializable;

public class InventorySummary implements Serializable {

    private Long locationId;

    private Location location;


    private Item item;

    private ItemPackageType itemPackageType;

    private Long quantity;

    private Boolean virtual;

    private InventoryStatus inventoryStatus;

    public InventorySummary(){}

    public InventorySummary(Inventory inventory) {
        setLocationId(inventory.getLocationId());
        setLocation(inventory.getLocation());
        setItem(inventory.getItem());
        setItemPackageType(inventory.getItemPackageType());
        setQuantity(inventory.getQuantity());
        setVirtual(inventory.getVirtual());
        setInventoryStatus(inventory.getInventoryStatus());
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
        if (location.getId() != null) {
            setLocationId(location.getId());
        }
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

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Double getSize() {

        ItemUnitOfMeasure stockItemUnitOfMeasure = itemPackageType.getStockItemUnitOfMeasures();

        return (quantity / stockItemUnitOfMeasure.getQuantity())
                * stockItemUnitOfMeasure.getLength()
                * stockItemUnitOfMeasure.getWidth()
                * stockItemUnitOfMeasure.getHeight();
    }

    public Boolean getVirtual() {
        return virtual;
    }

    public void setVirtual(Boolean virtual) {
        this.virtual = virtual;
    }


}
