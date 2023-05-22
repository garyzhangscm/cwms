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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "allocation_transaction_history")
public class AllocationTransactionHistory extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_transaction_history_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;
    @Column(name = "transaction_group_id")
    private String transactionGroupId;


    @Column(name = "order_number")
    private String orderNumber;

    @ManyToOne
    @JoinColumn(name = "shipment_line_id")
    private ShipmentLine shipmentLine;


    @Column(name = "work_order_number")
    private String workOrderNumber;
    @Column(name = "work_order_id")
    private Long workOrderId;
    @Transient
    private WorkOrder workOrder;

    @Column(name = "item_name")
    private String itemName;
    @Column(name = "item_id")
    private Long itemId;
    @Transient
    private Item item;

    @Column(name = "location_name")
    private String locationName;
    @Column(name = "location_id")
    private Long locationId;
    @Transient
    private Location location;

    // total required quantity from the
    // allocation request
    @Column(name = "total_required_quantity")
    private Long totalRequiredQuantity;
    // required quantity on this location, we may
    // already allocated something from other locations
    // so the current required quantity may be less
    // than the total required quantity
    @Column(name = "current_required_quantity")
    private Long currentRequiredQuantity;
    // total inventory quantity in this location
    @Column(name = "total_inventory_quantity")
    private Long totalInventoryQuantity;
    // total available quantity in this location.
    @Column(name = "total_available_quantity")
    private Long totalAvailableQuantity;

    @Column(name = "total_allocated_quantity")
    private Long totalAllocatedQuantity;
    @Column(name = "already_allocated_quantity")
    private Long alreadyAllocatedQuantity;


    // if the location is skipped due to some reason
    // reason will be in the message field
    @Column(name = "is_skipped_flag")
    private Boolean isSkippedFlag;

    @Column(name = "is_allocated_by_lpn_flag")
    private Boolean isAllocatedByLPNFlag;
    @Column(name = "is_round_up_flag")
    private Boolean isRoundUpFlag;

    @Column(name = "username")
    private String username;

    @Column(name = "message")
    private String message;


    @Column(name="color")
    private String color;

    @Column(name="product_size")
    private String productSize;

    @Column(name="style")
    private String style;

    // only allocate inventory that received by certain receipt
    @Column(name = "allocate_by_receipt_number")
    private String allocateByReceiptNumber;

    public static class Builder implements Serializable{

        private Long warehouseId;

        private String number;
        private String transactionGroupId;

        private String orderNumber;

        private ShipmentLine shipmentLine;

        private String workOrderNumber;
        private Long workOrderId;
        private WorkOrder workOrder;

        private String itemName;
        private Long itemId;
        private Item item;

        private String locationName;
        private Long locationId;
        private Location location;

        private Long totalRequiredQuantity;
        private Long currentRequiredQuantity;
        private Long totalInventoryQuantity;
        private Long totalAvailableQuantity;
        private Long totalAllocatedQuantity;
        private Long alreadyAllocatedQuantity;


        private Boolean isSkippedFlag;
        private Boolean isAllocatedByLPNFlag;
        private Boolean isRoundUpFlag;

        private String username;
        private String message;


        private String color;
        private String productSize;
        private String style;
        private String allocateByReceiptNumber;

        @Override
        public String toString() {
            try {
                return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
        }


        public Builder(Long warehouseId, String number,
                       String transactionGroupId) {
            this.warehouseId = warehouseId;
            this.number = number;
            this.transactionGroupId = transactionGroupId;
        }

        public Builder warehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
            return this;
        }
        public Builder number(String number) {
            this.number = number;
            return this;
        }
        public Builder transactionGroupId(String transactionGroupId) {
            this.transactionGroupId = transactionGroupId;
            return this;
        }

        public Builder orderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }

        public Builder shipmentLine(ShipmentLine shipmentLine) {
            this.shipmentLine = shipmentLine;
            if(Objects.nonNull(shipmentLine)) {
                this.color = shipmentLine.getOrderLine().getColor();
                this.style = shipmentLine.getOrderLine().getStyle();
                this.productSize = shipmentLine.getOrderLine().getProductSize();
                this.allocateByReceiptNumber = shipmentLine.getOrderLine().getAllocateByReceiptNumber();
            }
            return this;
        }

        public Builder workOrderNumber(String workOrderNumber) {
            this.workOrderNumber = workOrderNumber;
            return this;
        }

        public Builder workOrderId(Long workOrderId) {
            this.workOrderId = workOrderId;
            return this;
        }

        public Builder workOrder(WorkOrder workOrder) {
            this.workOrder = workOrder;
            return this;
        }

        public Builder itemName(String itemName) {
            this.itemName = itemName;
            return this;
        }

        public Builder itemId(Long itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder item(Item item) {
            this.item = item;
            return this;
        }

        public Builder locationName(String locationName) {
            this.locationName = locationName;
            return this;
        }

        public Builder locationId(Long locationId) {
            this.locationId = locationId;
            return this;
        }

        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        public Builder totalRequiredQuantity(Long totalRequiredQuantity) {
            this.totalRequiredQuantity = totalRequiredQuantity;
            return this;
        }

        public Builder currentRequiredQuantity(Long currentRequiredQuantity) {
            this.currentRequiredQuantity = currentRequiredQuantity;
            return this;
        }

        public Builder totalInventoryQuantity(Long totalInventoryQuantity) {
            this.totalInventoryQuantity = totalInventoryQuantity;
            return this;
        }

        public Builder totalAvailableQuantity(Long totalAvailableQuantity) {
            this.totalAvailableQuantity = totalAvailableQuantity;
            return this;
        }

        public Builder totalAllocatedQuantity(Long totalAllocatedQuantity) {
            this.totalAllocatedQuantity = totalAllocatedQuantity;
            return this;
        }
        public Builder alreadyAllocatedQuantity(Long alreadyAllocatedQuantity) {
            this.alreadyAllocatedQuantity = alreadyAllocatedQuantity;
            return this;
        }

        public Builder isSkippedFlag(Boolean isSkippedFlag) {
            this.isSkippedFlag = isSkippedFlag;
            return this;
        }
        public Builder isAllocatedByLPNFlag(Boolean isAllocatedByLPNFlag) {
            this.isAllocatedByLPNFlag = isAllocatedByLPNFlag;
            return this;
        }
        public Builder isRoundUpFlag(Boolean isRoundUpFlag) {
            this.isRoundUpFlag = isRoundUpFlag;
            return this;
        }


        public Builder username(String username) {
            this.username = username;
            return this;
        }
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public AllocationTransactionHistory build() {
            return new AllocationTransactionHistory(this);
        }

    }

    public AllocationTransactionHistory(){}

    private AllocationTransactionHistory(Builder builder) {
        warehouseId = builder.warehouseId;

        number = builder.number;
        transactionGroupId = builder.transactionGroupId;

        orderNumber = builder.orderNumber;
        shipmentLine = builder.shipmentLine;

        workOrderNumber = builder.workOrderNumber;
        workOrderId = builder.workOrderId;
        workOrder = builder.workOrder;

        itemName = builder.itemName;
        itemId = builder.itemId;
        item = builder.item;

        locationName = builder.locationName;
        locationId = builder.locationId;
        location = builder.location;

        totalRequiredQuantity = builder.totalRequiredQuantity;
        currentRequiredQuantity = builder.currentRequiredQuantity;
        totalInventoryQuantity = builder.totalInventoryQuantity;
        totalAvailableQuantity = builder.totalAvailableQuantity;
        totalAllocatedQuantity = builder.totalAllocatedQuantity;
        alreadyAllocatedQuantity = builder.alreadyAllocatedQuantity;

        isSkippedFlag = builder.isSkippedFlag;
        isAllocatedByLPNFlag = builder.isAllocatedByLPNFlag;
        isRoundUpFlag = builder.isRoundUpFlag;

        username = builder.username;
        message = builder.message;

        color = builder.color;
        style = builder.style;
        productSize = builder.productSize;
        allocateByReceiptNumber = builder.allocateByReceiptNumber;


    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTransactionGroupId() {
        return transactionGroupId;
    }

    public void setTransactionGroupId(String transactionGroupId) {
        this.transactionGroupId = transactionGroupId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public ShipmentLine getShipmentLine() {
        return shipmentLine;
    }

    public void setShipmentLine(ShipmentLine shipmentLine) {
        this.shipmentLine = shipmentLine;
    }

    public String getWorkOrderNumber() {
        return Strings.isNotBlank(workOrderNumber) ?
                workOrderNumber :
                     Objects.nonNull(workOrder)? workOrder.getNumber(): "" ;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public String getItemName() {
        return Strings.isNotBlank(itemName) ?
                itemName :
                Objects.nonNull(item) ? item.getName() : "";
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

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getLocationName() {
        return Strings.isNotBlank(locationName) ?
                locationName :
                Objects.nonNull(location) ? location.getName() : "";
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Long getTotalRequiredQuantity() {
        return totalRequiredQuantity;
    }

    public void setTotalRequiredQuantity(Long totalRequiredQuantity) {
        this.totalRequiredQuantity = totalRequiredQuantity;
    }

    public Long getCurrentRequiredQuantity() {
        return currentRequiredQuantity;
    }

    public void setCurrentRequiredQuantity(Long currentRequiredQuantity) {
        this.currentRequiredQuantity = currentRequiredQuantity;
    }

    public Long getTotalInventoryQuantity() {
        return totalInventoryQuantity;
    }

    public void setTotalInventoryQuantity(Long totalInventoryQuantity) {
        this.totalInventoryQuantity = totalInventoryQuantity;
    }

    public Long getTotalAvailableQuantity() {
        return totalAvailableQuantity;
    }

    public void setTotalAvailableQuantity(Long totalAvailableQuantity) {
        this.totalAvailableQuantity = totalAvailableQuantity;
    }

    public Long getTotalAllocatedQuantity() {
        return totalAllocatedQuantity;
    }

    public void setTotalAllocatedQuantity(Long totalAllocatedQuantity) {
        this.totalAllocatedQuantity = totalAllocatedQuantity;
    }

    public Boolean getSkippedFlag() {
        return isSkippedFlag;
    }

    public void setSkippedFlag(Boolean skippedFlag) {
        isSkippedFlag = skippedFlag;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Long getAlreadyAllocatedQuantity() {
        return alreadyAllocatedQuantity;
    }

    public void setAlreadyAllocatedQuantity(Long alreadyAllocatedQuantity) {
        this.alreadyAllocatedQuantity = alreadyAllocatedQuantity;
    }

    public Boolean getAllocatedByLPNFlag() {
        return isAllocatedByLPNFlag;
    }

    public void setAllocatedByLPNFlag(Boolean allocatedByLPNFlag) {
        isAllocatedByLPNFlag = allocatedByLPNFlag;
    }

    public Boolean getRoundUpFlag() {
        return isRoundUpFlag;
    }

    public void setRoundUpFlag(Boolean roundUpFlag) {
        isRoundUpFlag = roundUpFlag;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
