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

package com.garyzhangscm.cwms.inbound.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Inventory implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Inventory.class);


    private Long id;

    private Client client;

    private Long clientId;

    private String lpn;

    private Long locationId;

    private Item item;

    private ItemPackageType itemPackageType;

    private Long quantity;

    private Boolean virtual;

    private Long customerReturnOrderId;

    private Long customerReturnOrderLineId;

    private InventoryStatus inventoryStatus;

    private Location location;

    private Long receiptId;
    private String receiptNumber;
    private Long receiptLineId;
    private String receiptLineNumber;

    private List<InventoryMovement> inventoryMovements;

    private Long warehouseId;
    private Warehouse warehouse;

    private Boolean inboundQCRequired = false;


    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime fifoDate;

    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime inWarehouseDatetime;

    private String color;
    private String productSize;

    private String style;

    private String attribute1;
    private String attribute2;
    private String attribute3;
    private String attribute4;
    private String attribute5;


    private Boolean kitInventoryFlag = false;

    private Boolean kitInnerInventoryFlag = false;

    private Inventory kitInventory;

    private List<Inventory> kitInnerInventories = new ArrayList<>();

    public Double getSize() {
        if (itemPackageType == null) {
            logger.debug("Can't calcuate the inventory {}'s size as the item package type is null",
                    lpn);
            return 0.0;
        }

        ItemUnitOfMeasure stockItemUnitOfMeasure = itemPackageType.getStockItemUnitOfMeasure();
        if (Objects.isNull(stockItemUnitOfMeasure)) {
            logger.debug("Can't calcuate the inventory {}'s size as there's no stock item unit of measure defined for this item",
                    lpn);
            return 0.0;
        }

        return (quantity / stockItemUnitOfMeasure.getQuantity())
                * stockItemUnitOfMeasure.getLength()
                * stockItemUnitOfMeasure.getWidth()
                * stockItemUnitOfMeasure.getHeight();
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

    public Location getNextLocation() {
        if (Objects.isNull(inventoryMovements) ||
                inventoryMovements.size() == 0) {
            return null;
        }
        inventoryMovements.sort(Comparator.comparingLong(InventoryMovement::getSequence));
        return inventoryMovements.get(0).getLocation();
    }
    public Location getFinalLocation() {
        if (Objects.isNull(inventoryMovements) ||
                inventoryMovements.size() == 0) {
            return null;
        }
        inventoryMovements.sort(Comparator.comparingLong(InventoryMovement::getSequence));
        return inventoryMovements.get(inventoryMovements.size() - 1).getLocation();
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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
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

    public Long getReceiptLineId() {
        return receiptLineId;
    }

    public void setReceiptLineId(Long receiptLineId) {
        this.receiptLineId = receiptLineId;
    }

    public Boolean getInboundQCRequired() {
        return inboundQCRequired;
    }

    public void setInboundQCRequired(Boolean inboundQCRequired) {
        this.inboundQCRequired = inboundQCRequired;
    }

    public Long getCustomerReturnOrderId() {
        return customerReturnOrderId;
    }

    public void setCustomerReturnOrderId(Long customerReturnOrderId) {
        this.customerReturnOrderId = customerReturnOrderId;
    }

    public Long getCustomerReturnOrderLineId() {
        return customerReturnOrderLineId;
    }

    public void setCustomerReturnOrderLineId(Long customerReturnOrderLineId) {
        this.customerReturnOrderLineId = customerReturnOrderLineId;
    }

    public ZonedDateTime getFifoDate() {
        return fifoDate;
    }

    public void setFifoDate(ZonedDateTime fifoDate) {
        this.fifoDate = fifoDate;
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

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
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

    public ZonedDateTime getInWarehouseDatetime() {
        return inWarehouseDatetime;
    }

    public void setInWarehouseDatetime(ZonedDateTime inWarehouseDatetime) {
        this.inWarehouseDatetime = inWarehouseDatetime;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getReceiptLineNumber() {
        return receiptLineNumber;
    }

    public void setReceiptLineNumber(String receiptLineNumber) {
        this.receiptLineNumber = receiptLineNumber;
    }

    public Boolean getKitInventoryFlag() {
        return kitInventoryFlag;
    }

    public void setKitInventoryFlag(Boolean kitInventoryFlag) {
        this.kitInventoryFlag = kitInventoryFlag;
    }

    public Boolean getKitInnerInventoryFlag() {
        return kitInnerInventoryFlag;
    }

    public void setKitInnerInventoryFlag(Boolean kitInnerInventoryFlag) {
        this.kitInnerInventoryFlag = kitInnerInventoryFlag;
    }

    public Inventory getKitInventory() {
        return kitInventory;
    }

    public void setKitInventory(Inventory kitInventory) {
        this.kitInventory = kitInventory;
    }

    public List<Inventory> getKitInnerInventories() {
        return kitInnerInventories;
    }

    public void setKitInnerInventories(List<Inventory> kitInnerInventories) {
        this.kitInnerInventories = kitInnerInventories;
    }
}
