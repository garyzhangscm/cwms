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


import org.apache.logging.log4j.util.Strings;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public class Inventory extends AuditibleEntity<String> implements Serializable {


    private Long id;

    private String lpn = "";

    private Long locationId;

    private Location location;

    private Long receiptId;
    private Receipt receipt;

    private String orderNumber;

    private Item item;

    private ItemPackageType itemPackageType;

    private Long quantity = 0l;

    private Boolean virtual;

    private InventoryStatus inventoryStatus;

    // will be setup only when the inventory
    // is picked
    private Long pickId;
    private Pick pick;

    // Only when allocate by LPN happens
    private Long allocatedByPickId;

    // Only when allocate by LPN happens
    private Pick allocatedByPick;

    private Long warehouseId;


    private Warehouse warehouse;


    private String color = "";
    private String productSize = "";
    private String style = "";

    private String attribute1 = "";
    private String attribute2 = "";
    private String attribute3 = "";
    private String attribute4 = "";
    private String attribute5 = "";


    List<InventoryMovement> inventoryMovements = new ArrayList<>();

    private double caseQuantity = 0.0;

    private double packQuantity = 0.0;


    public void copyAttribute(Inventory anotherInvenotry) {
        setColor(anotherInvenotry.getColor());
        setProductSize(anotherInvenotry.getProductSize());
        setStyle(anotherInvenotry.getStyle());
        setAttribute1(anotherInvenotry.getAttribute1());
        setAttribute2(anotherInvenotry.getAttribute2());
        setAttribute3(anotherInvenotry.getAttribute3());
        setAttribute4(anotherInvenotry.getAttribute4());
        setAttribute5(anotherInvenotry.getAttribute5());
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

        if (Objects.isNull(itemPackageType)) {
            return 0.0;
        }
        ItemUnitOfMeasure stockItemUnitOfMeasure = itemPackageType.getStockItemUnitOfMeasure();
        if (Objects.isNull(stockItemUnitOfMeasure)) {
            return 0.0;
        }

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

    public Long getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Long receiptId) {
        this.receiptId = receiptId;
    }

    public Long getPickId() {
        return pickId;
    }

    public void setPickId(Long pickId) {
        this.pickId = pickId;
    }

    public Pick getPick() {
        return pick;
    }

    public void setPick(Pick pick) {
        this.pick = pick;
    }

    public List<InventoryMovement> getInventoryMovements() {
        return inventoryMovements;
    }

    public void setInventoryMovements(List<InventoryMovement> inventoryMovements) {
        this.inventoryMovements = inventoryMovements;
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

    public Long getAllocatedByPickId() {
        return allocatedByPickId;
    }

    public void setAllocatedByPickId(Long allocatedByPickId) {
        this.allocatedByPickId = allocatedByPickId;
    }

    public Pick getAllocatedByPick() {
        return allocatedByPick;
    }

    public void setAllocatedByPick(Pick allocatedByPick) {
        this.allocatedByPick = allocatedByPick;
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

    public Receipt getReceipt() {
        return receipt;
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    public long getQuantityPerCase() {
        if (Objects.isNull(getItemPackageType()) ||
                Objects.isNull(getItemPackageType().getCaseItemUnitOfMeasure())) {
            return 0;
        }
        return getItemPackageType().getCaseItemUnitOfMeasure().getQuantity();
    }


    public double getCaseQuantity() {
        if (caseQuantity > 0) {
            return caseQuantity;
        }
        long quantityPerCase = getQuantityPerCase();
        if (quantityPerCase == 0) {
            return 0;
        }
        return getQuantity() * 1.0 / quantityPerCase;
    }
    public long getQuantityPerPack() {
        if (Objects.isNull(getItemPackageType()) ||
                Objects.isNull(getItemPackageType().getPackItemUnitOfMeasure())) {
            return 0;
        }
        return getItemPackageType().getPackItemUnitOfMeasure().getQuantity();
    }

    public String getOrderNumber() {
        if (Strings.isNotBlank(orderNumber)) {
            return  orderNumber;
        }
        if (Objects.nonNull(pick)) {
            return pick.getOrderNumber();
        }
        return "";
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public double getPackQuantity() {
        if (packQuantity > 0) {
            return packQuantity;
        }
        long quantityPerPack= getQuantityPerPack();
        if (quantityPerPack == 0) {
            return 0;
        }
        return getQuantity() * 1.0 / quantityPerPack;
    }

    public long getPackPerCase() {

        long quantityPerPack= getQuantityPerPack();
        if (quantityPerPack == 0) {
            return 0;
        }
        long quantityPerCase = getQuantityPerCase();
        return quantityPerCase / quantityPerPack;

    }

    public void setCaseQuantity(double caseQuantity) {
        this.caseQuantity = caseQuantity;
    }

    public void setPackQuantity(double packQuantity) {
        this.packQuantity = packQuantity;
    }
}
