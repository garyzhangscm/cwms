package com.garyzhangscm.cwms.adminserver.model.wms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

public class ShortAllocation  implements Serializable {

    private Long id;

    private ShipmentLine shipmentLine;

    private String orderNumber;

    private String workOrderNumber;

    private Long warehouseId;

    private Warehouse warehouse;


    // re-allocate the short allocation based upon
    // the last allocation time, to make sure we
    // will only re-try after certain amount time
    // has been passed.
    private ZonedDateTime lastAllocationDatetime;

    private List<Pick> picks;

    private Long itemId;

    private Item item;

    private Long quantity;

    private Long openQuantity;

    private Long inprocessQuantity;

    private Long deliveredQuantity;

    private Long allocationCount;

    private ShortAllocationStatus status;


    private Long workOrderLineId;

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



    public ShortAllocationStatus getStatus() {
        return status;
    }

    public void setStatus(ShortAllocationStatus status) {
        this.status = status;
    }

    public List<Pick> getPicks() {
        return picks;
    }

    public void setPicks(List<Pick> picks) {
        this.picks = picks;
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

    public Long getAllocationCount() {
        return allocationCount;
    }

    public void setAllocationCount(Long allocationCount) {
        this.allocationCount = allocationCount;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public ZonedDateTime getLastAllocationDatetime() {
        return lastAllocationDatetime;
    }

    public void setLastAllocationDatetime(ZonedDateTime lastAllocationDatetime) {
        this.lastAllocationDatetime = lastAllocationDatetime;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }
}
