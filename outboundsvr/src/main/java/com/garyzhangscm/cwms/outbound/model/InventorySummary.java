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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;

public class InventorySummary implements Serializable {

    private Long locationId;

    private Location location;


    private Item item;

    private ItemPackageType itemPackageType;

    private Long quantity;

    private Boolean virtual;

    private InventoryStatus inventoryStatus;


    private ZonedDateTime fifoDate;

    // map of inventory.
    // key: LPN
    // value: inventory
    private Map<String, List<Inventory>> inventories = new HashMap<>();

    private String color;
    private String productSize;
    private String style;

    private String attribute1;
    private String attribute2;
    private String attribute3;
    private String attribute4;
    private String attribute5;

    private String allocateByReceiptNumber;

    public InventorySummary(){}

    public InventorySummary(Inventory inventory) {
        setLocationId(inventory.getLocationId());
        setLocation(inventory.getLocation());
        setItem(inventory.getItem());
        setItemPackageType(inventory.getItemPackageType());
        setQuantity(inventory.getQuantity());
        setVirtual(inventory.getVirtual());
        setInventoryStatus(inventory.getInventoryStatus());
        setFifoDate(inventory.getCreatedTime());
        setColor(inventory.getColor());
        setProductSize(inventory.getProductSize());
        setStyle(inventory.getStyle());
        setAttribute1(inventory.getAttribute1());
        setAttribute2(inventory.getAttribute2());
        setAttribute3(inventory.getAttribute3());
        setAttribute4(inventory.getAttribute4());
        setAttribute5(inventory.getAttribute5());
        addInventory(inventory);
    }

    public void resetFIFODate(ZonedDateTime newFifoDate) {
        // reset the FIFO date is the new fifo date is older
        // than the inventory summary.
        // we will make sure there's only one FIFO date for the
        // whole inventory summary(no matter how many inventory record
        // this summary includes) and the FIFO date is always
        // the oldest inventory inside the summary
        if (getFifoDate().isAfter(newFifoDate)) {
            setFifoDate(newFifoDate);
        }
    }

    public void addInventory(Inventory inventory) {

        List<Inventory> inventoryList = inventories.getOrDefault(inventory.getLpn(), new ArrayList<>());
        inventoryList.add(inventory);
        inventories.put(inventory.getLpn(), inventoryList);
    }

    public void markLPNAsAllocated(String lpn, Long pickId) {
        List<Inventory> inventoryList = inventories.getOrDefault(lpn, new ArrayList<>());
        inventoryList.forEach(inventory -> inventory.setAllocatedByPickId(pickId));

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

    public String getAttribute1() {
        return attribute1;
    }

    public void setAttribute1(String attribute1) {
        this.attribute1 = attribute1;
    }

    public String getAttribute2() {
        return attribute2;
    }

    public void setAttribute2(String attribute2) {
        this.attribute2 = attribute2;
    }

    public String getAttribute3() {
        return attribute3;
    }

    public void setAttribute3(String attribute3) {
        this.attribute3 = attribute3;
    }

    public String getAttribute4() {
        return attribute4;
    }

    public void setAttribute4(String attribute4) {
        this.attribute4 = attribute4;
    }

    public String getAttribute5() {
        return attribute5;
    }

    public void setAttribute5(String attribute5) {
        this.attribute5 = attribute5;
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

        ItemUnitOfMeasure stockItemUnitOfMeasure = itemPackageType.getStockItemUnitOfMeasure();

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

    public ZonedDateTime getFifoDate() {
        return fifoDate;
    }

    public void setFifoDate(ZonedDateTime fifoDate) {
        this.fifoDate = fifoDate;
    }

    public Map<String, List<Inventory>> getInventories() {
        return inventories;
    }

    public void setInventories(Map<String, List<Inventory>> inventories) {
        this.inventories = inventories;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getProductSize() {
        return productSize;
    }

    public void setProductSize(String productSize) {
        this.productSize = productSize;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getAllocateByReceiptNumber() {
        return allocateByReceiptNumber;
    }

    public void setAllocateByReceiptNumber(String allocateByReceiptNumber) {
        this.allocateByReceiptNumber = allocateByReceiptNumber;
    }
}
