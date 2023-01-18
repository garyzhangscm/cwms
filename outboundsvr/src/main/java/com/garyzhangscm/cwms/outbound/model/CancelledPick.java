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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "cancelled_pick")
public class CancelledPick  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cancelled_pick_id")
    @JsonProperty(value="id")
    private Long id;

    // We will only save the pickId here, When we cancelled
    // the pick, we may remove the pick record as well if
    // the whole pick is cancelled
    @Column(name = "pick_id")
    private Long pickId;

    @Column(name = "pick_number")
    private String pickNumber;

    @Column(name = "number", unique = true)
    private String number;

    @Column(name = "source_location_id")
    private Long sourceLocationId;

    @Transient
    private Location sourceLocation;

    @Column(name = "destination_location_id")
    private Long destinationLocationId;

    @Transient
    private Location destinationLocation;

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "shipment_line_id")
    @JsonIgnore
    private ShipmentLine shipmentLine;

    @Column(name = "work_order_line_id")
    private Long workOrderLineId;

    @ManyToOne
    @JoinColumn(name = "short_allocation_id")
    @JsonIgnore
    private ShortAllocation shortAllocation;

    @Column(name = "type")
    private PickType pickType;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "picked_quantity")
    private Long pickedQuantity;

    @Column(name = "cancelled_quantity")
    private Long cancelledQuantity;

    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;

    @Transient
    private InventoryStatus inventoryStatus;

    @ManyToOne
    @JoinColumn(name = "pick_list_id")
    private PickList pickList;


    @ManyToOne
    @JoinColumn(name = "cartonization_id")
    @JsonIgnore
    private Cartonization cartonization;


    @Column(name = "cancelled_username")
    private String cancelledUsername;

    @Transient
    private User cancelledUser;

    @Column(name = "cancelled_date")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime cancelledDate;
    // private LocalDateTime cancelledDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPickId() {
        return pickId;
    }

    public void setPickId(Long pickId) {
        this.pickId = pickId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public Location getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(Location sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(Long destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
        this.destinationLocation = destinationLocation;
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


    public Long getWorkOrderLineId() {
        return workOrderLineId;
    }

    public void setWorkOrderLineId(Long workOrderLineId) {
        this.workOrderLineId = workOrderLineId;
    }

    public ShipmentLine getShipmentLine() {
        return shipmentLine;
    }

    public void setShipmentLine(ShipmentLine shipmentLine) {
        this.shipmentLine = shipmentLine;
    }

    public ShortAllocation getShortAllocation() {
        return shortAllocation;
    }

    public void setShortAllocation(ShortAllocation shortAllocation) {
        this.shortAllocation = shortAllocation;
    }

    public PickType getPickType() {
        return pickType;
    }

    public void setPickType(PickType pickType) {
        this.pickType = pickType;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getPickedQuantity() {
        return pickedQuantity;
    }

    public void setPickedQuantity(Long pickedQuantity) {
        this.pickedQuantity = pickedQuantity;
    }

    public Long getCancelledQuantity() {
        return cancelledQuantity;
    }

    public void setCancelledQuantity(Long cancelledQuantity) {
        this.cancelledQuantity = cancelledQuantity;
    }

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public PickList getPickList() {
        return pickList;
    }

    public void setPickList(PickList pickList) {
        this.pickList = pickList;
    }

    public String getCancelledUsername() {
        return cancelledUsername;
    }

    public void setCancelledUsername(String cancelledUsername) {
        this.cancelledUsername = cancelledUsername;
    }

    public User getCancelledUser() {
        return cancelledUser;
    }

    public void setCancelledUser(User cancelledUser) {
        this.cancelledUser = cancelledUser;
    }

    public ZonedDateTime getCancelledDate() {
        return cancelledDate;
    }

    public void setCancelledDate(ZonedDateTime cancelledDate) {
        this.cancelledDate = cancelledDate;
    }

    public String getPickNumber() {
        return pickNumber;
    }

    public void setPickNumber(String pickNumber) {
        this.pickNumber = pickNumber;
    }


    public Cartonization getCartonization() {
        return cartonization;
    }

    public void setCartonization(Cartonization cartonization) {
        this.cartonization = cartonization;
    }
    public String getCartonizationNumber(){
        return Objects.isNull(getCartonization()) ? "" : getCartonization().getNumber();
    }
}
