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

package com.garyzhangscm.cwms.inventory.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "inventory")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer"}, ignoreUnknown = true)
public class Inventory extends AuditibleEntity<String> implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Inventory.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "lpn")
    private String lpn;


    @JsonIgnore
    @Transient
    private Client client;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "location_id")
    private Long locationId;

    @JsonIgnore
    @Transient
    private Location location;

    @Transient
    private String locationName;

    @Column(name = "pick_id")
    private Long pickId;

    @JsonIgnore
    @Transient
    private Pick pick;

    @Transient
    private String pickNumber;

    // Only when allocate by LPN happens
    @Column(name = "allocated_by_pick_id")
    private Long allocatedByPickId;

    // Only when allocate by LPN happens
    @JsonIgnore
    @Transient
    private Pick allocatedByPick;

    // if the inventory is received from receipt
    @Column(name = "receipt_id")
    private Long receiptId;

    @JsonIgnore
    @Transient
    private Receipt receipt;

    @Transient
    private String receiptNumber;

    @Column(name = "receipt_line_id")
    private Long receiptLineId;

    @JsonIgnore
    @Transient
    private ReceiptLine receiptLine;

    // if the inventory is received from the customer return
    @Column(name = "customer_return_order_id")
    private Long customerReturnOrderId;

    @Column(name = "customer_return_order_line_id")
    private Long customerReturnOrderLineId;

    // When inventory is produced by some work order
    @Column(name = "work_order_id")
    private Long workOrderId;

    @JsonIgnore
    @Transient
    private WorkOrder workOrder;


    // When we return material from some work order line
    @Column(name = "work_order_line_id")
    private Long workOrderLineId;


    // When we have some by product from the work order
    @Column(name = "work_order_by_product_id")
    private Long workOrderByProductId;

    // the transaction id that created this inventory
    @Column(name = "create_inventory_transaction_id")
    private Long createInventoryTransactionId;

    @ManyToOne
    @JoinColumn(name="item_id")
    private Item item;

    @Transient
    private Long itemId;

    @Transient
    private String itemName;


    @ManyToOne
    @JoinColumn(name="item_package_type_id")
    private ItemPackageType itemPackageType;

    @Transient
    private Long itemtemPackageTypeId;

    @Transient
    private String itemPackageTypeName;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "virtual_inventory")
    private Boolean virtual;

    @ManyToOne
    @JoinColumn(name="inventory_status_id")
    private InventoryStatus inventoryStatus;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    @JsonIgnore
    private Warehouse warehouse;

    @Column(name="color")
    private String color;

    @Column(name="product_size")
    private String productSize;

    @Column(name="style")
    private String style;

    @Column(name="attribute_1")
    private String attribute1;
    @Column(name="attribute_2")
    private String attribute2;
    @Column(name="attribute_3")
    private String attribute3;
    @Column(name="attribute_4")
    private String attribute4;
    @Column(name="attribute_5")
    private String attribute5;

    @Column(name = "reason_code_id")
    private Long reasonCodeId;

    @Transient
    private ReasonCode reasonCode;

    // date used when we will need to sort the inventory based on fifo
    @Column(name = "fifo_date")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime fifoDate;

    // date used when we will use to keep inventory aging
    @Column(name = "in_warehouse_datetime")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime inWarehouseDatetime;

    // date used when we will use to keep inventory aging
    @Column(name = "shipped_datetime")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime shippedDatetime;

    @OneToMany(
            mappedBy = "inventory",
            cascade = CascadeType.ALL,
            // orphanRemoval = true, // We will process the movement manually from InventoryMovementService
            fetch = FetchType.LAZY
    )
    private List<InventoryWithLock> locks = new ArrayList<>();

    @Column(name = "locked_for_adjust")
    private Boolean lockedForAdjust = false;


    @Column(name = "inbound_qc_required")
    private Boolean inboundQCRequired = false;

    // if this inventory is kit item
    @Column(name = "kit_inventory_flag")
    private Boolean kitInventoryFlag = false;

    // if this inventory is in a kit
    @Column(name = "kit_inner_inventory_flag")
    private Boolean kitInnerInventoryFlag = false;

    // if this inventory is in a kit,
    // point to the kit inventory
    @ManyToOne(cascade=CascadeType.PERSIST)
    @JoinColumn(name="kit_inventory_id")
    @JsonIgnore
    private Inventory kitInventory;

    @OneToMany(mappedBy="kitInventory")
    @JsonIgnore
    private List<Inventory> kitInnerInventories = new ArrayList<>();

    @OneToMany(
            mappedBy = "inventory",
            cascade = CascadeType.REMOVE,
            // orphanRemoval = true, // We will process the movement manually from InventoryMovementService
            fetch = FetchType.LAZY
    )
    List<InventoryMovement> inventoryMovements = new ArrayList<>();


    // date used when we will use to keep inventory aging
    @Column(name = "last_qc_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime lastQCTime;

    // flag to make the inventory for remove
    @Column(name = "marked_for_remove")
    private Boolean markedForRemove;

    public Inventory split(String newLpn, Long newInventoryQuantity) {
        Inventory inventory = new Inventory();
        if (StringUtils.isBlank(newLpn)) {
            newLpn = getLpn();
        }
        inventory.setLpn(newLpn);
        inventory.setPickId(getPickId());
        inventory.setWarehouseId(warehouseId);
        // Copy inventory movement
        List<InventoryMovement> inventoryMovements = new ArrayList<>();
        getInventoryMovements().stream().forEach(inventoryMovement -> {
            InventoryMovement newInventoryMovement = (InventoryMovement)inventoryMovement.clone();
            newInventoryMovement.setInventory(inventory);
            newInventoryMovement.setWarehouseId(inventoryMovement.getWarehouseId());
            inventoryMovements.add(newInventoryMovement);
        });

        inventory.setInventoryMovements(inventoryMovements);

        inventory.setInventoryStatus(getInventoryStatus());
        inventory.setItem(getItem());
        inventory.setItemPackageType(getItemPackageType());
        inventory.setLocationId(getLocationId());
        inventory.setLocation(getLocation());
        inventory.setQuantity(newInventoryQuantity);
        inventory.setWorkOrderId(getWorkOrderId());
        inventory.setVirtual(getVirtual());
        inventory.setReceiptId(getReceiptId());
        inventory.setReceiptLineId(getReceiptLineId());
        inventory.setClientId(getClientId());

        inventory.setColor(getColor());
        inventory.setProductSize(getProductSize());
        inventory.setStyle(getStyle());

        inventory.setAttribute1(getAttribute1());
        inventory.setAttribute2(getAttribute2());
        inventory.setAttribute3(getAttribute3());
        inventory.setAttribute4(getAttribute4());
        inventory.setAttribute5(getAttribute5());

        inventory.setFifoDate(getFifoDate());

        setQuantity(getQuantity() - newInventoryQuantity);

        inventory.setInWarehouseDatetime(getInWarehouseDatetime());
        inventory.setShippedDatetime(getShippedDatetime());

        inventory.setKitInventoryFlag(getKitInventoryFlag());

        inventory.setKitInnerInventoryFlag(getKitInnerInventoryFlag());

        // if we are split a kit inventory, then we will
        // need to split all the inner inventory into the new kit
        // Note: We will always assume there's only one level of kit - inner
        // which means if this is a kit inner inventory, then it won't be
        // a kit inventory that contains other inventory
        if (Boolean.TRUE.equals(inventory.getKitInventory())) {
            List<Inventory> innerInventoryInNewKitInventory = new ArrayList<>();
            for (Inventory kitInnerInventory : inventory.getKitInnerInventories()) {

                Long newKitInnerInventoryQuantity = newInventoryQuantity * kitInnerInventory.getQuantity() / getQuantity();
                Inventory newKitInnerInventory = kitInnerInventory.split(newLpn, newKitInnerInventoryQuantity);
                newKitInnerInventory.setKitInventory(inventory);
                innerInventoryInNewKitInventory.add(newKitInnerInventory);
                // in case of split a kit inventory, we will need to update the inner inventory's quantity as well
                kitInnerInventory.setQuantity(kitInnerInventory.getQuantity() - newKitInnerInventoryQuantity);
            }
            inventory.setKitInnerInventories(innerInventoryInNewKitInventory);

        }
        // if we are split a inventory inside the kit, then copy the reference
        // to the kit inventory to the new inner kit inventory
        if (Boolean.TRUE.equals(inventory.getKitInnerInventoryFlag())) {
            inventory.setKitInventory(getKitInventory());
        }

        return inventory;

    }


    public Inventory copy(String newLpn, Long newQuantity) {
        Inventory inventory = new Inventory();
        if (StringUtils.isBlank(newLpn)) {
            newLpn = getLpn();
        }
        inventory.setLpn(newLpn);
        // inventory.setPickId(getPickId());
        inventory.setWarehouseId(warehouseId);
        // Copy inventory movement
        List<InventoryMovement> inventoryMovements = new ArrayList<>();
        getInventoryMovements().stream().forEach(inventoryMovement -> {
            InventoryMovement newInventoryMovement = (InventoryMovement)inventoryMovement.clone();
            newInventoryMovement.setInventory(inventory);
            newInventoryMovement.setWarehouseId(inventoryMovement.getWarehouseId());
            inventoryMovements.add(newInventoryMovement);
        });

        inventory.setInventoryMovements(inventoryMovements);

        inventory.setInventoryStatus(getInventoryStatus());
        inventory.setItem(getItem());
        inventory.setItemPackageType(getItemPackageType());
        inventory.setLocationId(getLocationId());
        inventory.setLocation(getLocation());
        inventory.setQuantity(newQuantity);
        inventory.setWorkOrderId(getWorkOrderId());
        inventory.setVirtual(getVirtual());
        inventory.setReceiptId(getReceiptId());
        inventory.setReceiptLineId(getReceiptLineId());
        inventory.setClientId(getClientId());

        inventory.setColor(getColor());
        inventory.setProductSize(getProductSize());
        inventory.setStyle(getStyle());

        inventory.setAttribute1(getAttribute1());
        inventory.setAttribute2(getAttribute2());
        inventory.setAttribute3(getAttribute3());
        inventory.setAttribute4(getAttribute4());
        inventory.setAttribute5(getAttribute5());

        inventory.setFifoDate(getFifoDate());

        inventory.setInWarehouseDatetime(getInWarehouseDatetime());
        inventory.setShippedDatetime(getShippedDatetime());

        inventory.setKitInventoryFlag(getKitInventoryFlag());

        inventory.setKitInnerInventoryFlag(getKitInnerInventoryFlag());

        return inventory;

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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inventory inventory = (Inventory) o;
        return Objects.equals(id, inventory.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
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
        if (Objects.nonNull(location) &&
                Objects.nonNull(location.getId()) &&
                Objects.isNull(getLocationId())) {
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

    public Long getReceiptLineId() {
        return receiptLineId;
    }

    public void setReceiptLineId(Long receiptLineId) {
        this.receiptLineId = receiptLineId;
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
        if (Objects.isNull(inventoryMovements)) {
            inventoryMovements = new ArrayList<>();
        }
        return inventoryMovements;
    }

    public void setInventoryMovements(List<InventoryMovement> inventoryMovements) {
        this.inventoryMovements = inventoryMovements;
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

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Long getWorkOrderLineId() {
        return workOrderLineId;
    }

    public void setWorkOrderLineId(Long workOrderLineId) {
        this.workOrderLineId = workOrderLineId;
    }

    public Long getWorkOrderByProductId() {
        return workOrderByProductId;
    }

    public void setWorkOrderByProductId(Long workOrderByProductId) {
        this.workOrderByProductId = workOrderByProductId;
    }

    public static Logger getLogger() {
        return logger;
    }

    public Long getAllocatedByPickId() {
        return allocatedByPickId;
    }

    public void setAllocatedByPickId(Long allocatedByPickId) {
        this.allocatedByPickId = allocatedByPickId;
    }

    public ZonedDateTime getShippedDatetime() {
        return shippedDatetime;
    }

    public void setShippedDatetime(ZonedDateTime shippedDatetime) {
        this.shippedDatetime = shippedDatetime;
    }

    public Pick getAllocatedByPick() {
        return allocatedByPick;
    }

    public void setAllocatedByPick(Pick allocatedByPick) {
        this.allocatedByPick = allocatedByPick;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public Long getCreateInventoryTransactionId() {
        return createInventoryTransactionId;
    }

    public void setCreateInventoryTransactionId(Long createInventoryTransactionId) {
        this.createInventoryTransactionId = createInventoryTransactionId;
    }

    public ZonedDateTime getInWarehouseDatetime() {
        return inWarehouseDatetime;
    }

    public void setInWarehouseDatetime(ZonedDateTime inWarehouseDatetime) {
        this.inWarehouseDatetime = inWarehouseDatetime;
    }

    public ZonedDateTime getFifoDate() {
        return fifoDate;
    }

    public void setFifoDate(ZonedDateTime fifoDate) {
        this.fifoDate = fifoDate;
    }

    public Boolean getInboundQCRequired() {
        return inboundQCRequired;
    }

    public void setInboundQCRequired(Boolean inboundQCRequired) {
        this.inboundQCRequired = inboundQCRequired;
    }

    public List<InventoryWithLock> getLocks() {
        return locks;
    }

    public void setLocks(List<InventoryWithLock> locks) {
        this.locks = locks;
    }

    public boolean isLocked() {
        return Objects.nonNull(getLocks())  &&
                getLocks().size() >0;
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

    public Receipt getReceipt() {
        return receipt;
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    public ReceiptLine getReceiptLine() {
        return receiptLine;
    }

    public void setReceiptLine(ReceiptLine receiptLine) {
        this.receiptLine = receiptLine;
    }

    public Long getReasonCodeId() {
        return reasonCodeId;
    }

    public void setReasonCodeId(Long reasonCodeId) {
        this.reasonCodeId = reasonCodeId;
    }

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(ReasonCode reasonCode) {
        this.reasonCode = reasonCode;
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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Long getItemId() {
        if (Objects.nonNull(itemId)) {
            return itemId;
        }
        if (Objects.nonNull(item)) {
            return item.getId();
        }
        return null;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {

        if (Strings.isNotBlank(itemName)) {
            return itemName;
        }
        if (Objects.nonNull(item)) {
            return item.getName();
        }
        return "";
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getItemtemPackageTypeId() {
        if (Objects.nonNull(itemtemPackageTypeId)) {
            return itemtemPackageTypeId;
        }
        if (Objects.nonNull(itemPackageType)) {
            return itemPackageType.getId();
        }
        return null;
    }

    public void setItemtemPackageTypeId(Long itemtemPackageTypeId) {
        this.itemtemPackageTypeId = itemtemPackageTypeId;
    }

    public String getItemPackageTypeName() {

        if (Strings.isNotBlank(itemPackageTypeName)) {
            return itemPackageTypeName;
        }
        if (Objects.nonNull(itemPackageType)) {
            return itemPackageType.getName();
        }
        return "";
    }

    public void setItemPackageTypeName(String itemPackageTypeName) {
        this.itemPackageTypeName = itemPackageTypeName;
    }

    public String getLocationName() {
        if(Strings.isNotBlank(locationName)) {
            return locationName;
        }
        if (Objects.nonNull(location)) {
            return location.getName();
        }
        return "";
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getPickNumber() {
        if(Strings.isNotBlank(pickNumber)) {
            return pickNumber;
        }
        if (Objects.nonNull(pick)) {
            return pick.getNumber();
        }
        return "";
    }

    public void setPickNumber(String pickNumber) {
        this.pickNumber = pickNumber;
    }

    public String getReceiptNumber() {

        if(Strings.isNotBlank(receiptNumber)) {
            return receiptNumber;
        }
        if (Objects.nonNull(receipt)) {
            return receipt.getNumber();
        }
        return "";
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public ZonedDateTime getLastQCTime() {
        return lastQCTime;
    }

    public void setLastQCTime(ZonedDateTime lastQCTime) {
        this.lastQCTime = lastQCTime;
    }

    public Boolean getMarkedForRemove() {
        return markedForRemove;
    }

    public void setMarkedForRemove(Boolean markedForRemove) {
        this.markedForRemove = markedForRemove;
    }
}
