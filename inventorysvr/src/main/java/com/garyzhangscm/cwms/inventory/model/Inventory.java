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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
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

    @Transient
    private Client client;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "location_id")
    private Long locationId;

    @Transient
    private Location location;

    @Column(name = "pick_id")
    private Long pickId;

    @Transient
    private Pick pick;

    // Only when allocate by LPN happens
    @Column(name = "allocated_by_pick_id")
    private Long allocatedByPickId;

    // Only when allocate by LPN happens
    @Transient
    private Pick allocatedByPick;

    // if the inventory is received from receipt
    @Column(name = "receipt_id")
    private Long receiptId;

    @Transient
    private Receipt receipt;

    @Column(name = "receipt_line_id")
    private Long receiptLineId;

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

    @ManyToOne
    @JoinColumn(name="item_package_type_id")
    private ItemPackageType itemPackageType;

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

    @OneToMany(
            mappedBy = "inventory",
            cascade = CascadeType.REMOVE,
            // orphanRemoval = true, // We will process the movement manually from InventoryMovementService
            fetch = FetchType.LAZY
    )
    List<InventoryMovement> inventoryMovements = new ArrayList<>();

    public Inventory split(String newLpn, Long newQuantity) {
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
        inventory.setQuantity(newQuantity);
        inventory.setReceiptId(getReceiptId());
        inventory.setWorkOrderId(getWorkOrderId());
        inventory.setVirtual(getVirtual());
        inventory.setReceiptId(getReceiptId());
        inventory.setReceiptLineId(getReceiptLineId());
        inventory.setClientId(getClientId());

        inventory.setColor(getColor());
        inventory.setProductSize(getProductSize());
        inventory.setColor(getColor());

        inventory.setAttribute1(getAttribute1());
        inventory.setAttribute2(getAttribute2());
        inventory.setAttribute3(getAttribute3());
        inventory.setAttribute4(getAttribute4());
        inventory.setAttribute5(getAttribute5());

        inventory.setFifoDate(getFifoDate());

        setQuantity(getQuantity() - newQuantity);

        return inventory;

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

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
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
}
