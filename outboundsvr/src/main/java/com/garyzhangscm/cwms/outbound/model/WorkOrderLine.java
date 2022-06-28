package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

public class WorkOrderLine {
    private Long id;

    private String number;

    private Long itemId;

    private Item item;

    private String workOrderNumber;

    private Long expectedQuantity;

    private Long openQuantity;

    private Long inprocessQuantity;

    private Long consumedQuantity;
    private Long inventoryStatusId;

    private InventoryStatus inventoryStatus;

    private AllocationStrategyType allocationStrategyType;
    private Long warehouseId;

    private Warehouse warehouse;
    private List<WorkOrderLineSparePart> workOrderLineSpareParts = new ArrayList<>();

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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
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

    public Long getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(Long consumedQuantity) {
        this.consumedQuantity = consumedQuantity;
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

    public AllocationStrategyType getAllocationStrategyType() {
        return allocationStrategyType;
    }

    public void setAllocationStrategyType(AllocationStrategyType allocationStrategyType) {
        this.allocationStrategyType = allocationStrategyType;
    }

    public List<WorkOrderLineSparePart> getWorkOrderLineSpareParts() {
        return workOrderLineSpareParts;
    }

    public void setWorkOrderLineSpareParts(List<WorkOrderLineSparePart> workOrderLineSpareParts) {
        this.workOrderLineSpareParts = workOrderLineSpareParts;
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

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }
}
