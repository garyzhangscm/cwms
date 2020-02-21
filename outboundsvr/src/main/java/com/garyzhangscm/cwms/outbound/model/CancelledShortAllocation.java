package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cancelled_short_allocation")
public class CancelledShortAllocation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cancelled_allocation_id")
    @JsonProperty(value="id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "shipment_line_id")
    @JsonIgnore
    private ShipmentLine shipmentLine;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;


    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "open_quantity")
    private Long openQuantity;

    @Column(name = "inprocess_quantity")
    private Long inprocessQuantity;

    @Column(name = "delivered_quantity")
    private Long deliveredQuantity;

    // How many times we have tried to
    // allocate this short allocation to
    // get an emergency replenishment
    @Column(name = "allocation_count")
    private Long allocationCount;

    @Column(name = "status")
    private ShortAllocationStatus status;


    @Column(name = "work_order_line_id")
    private Long workOrderLineId;


    @Column(name = "cancelled_quantity")
    private Long cancelledQuantity;

    @Column(name = "cancelled_username")
    private String cancelledUsername;

    @Transient
    private User cancelledUser;

    @Column(name = "cancelled_date")
    private LocalDateTime cancelledDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public ShipmentLine getShipmentLine() {
        return shipmentLine;
    }

    public void setShipmentLine(ShipmentLine shipmentLine) {
        this.shipmentLine = shipmentLine;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getOrderNumber() {
        return shipmentLine == null ? "" : shipmentLine.getOrderNumber();
    }

    public ShortAllocationStatus getStatus() {
        return status;
    }

    public void setStatus(ShortAllocationStatus status) {
        this.status = status;
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

    public Long getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Long openQuantity) {
        this.openQuantity = openQuantity;
    }

    public Long getInprocessQuantity() {
        return inprocessQuantity;
    }

    public void setInprocessQuantity(Long inprocessQuantity) {
        this.inprocessQuantity = inprocessQuantity;
    }

    public Long getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(Long deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }

    public Long getCancelledQuantity() {
        return cancelledQuantity;
    }

    public void setCancelledQuantity(Long cancelledQuantity) {
        this.cancelledQuantity = cancelledQuantity;
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

    public LocalDateTime getCancelledDate() {
        return cancelledDate;
    }

    public void setCancelledDate(LocalDateTime cancelledDate) {
        this.cancelledDate = cancelledDate;
    }

    public Long getAllocationCount() {
        return allocationCount;
    }

    public void setAllocationCount(Long allocationCount) {
        this.allocationCount = allocationCount;
    }
}
