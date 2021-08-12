package com.garyzhangscm.cwms.workorder.model;

public class ProductionLineAllocationRequestLine {

    Long workOrderLineId;
    Long totalQuantity; // total assigned quantity, read from production line assignment
    Long openQuantity;  // total open quantity, read from production line assignment
    Long allocatingQuantity; // quantity to be allocated

    public Long getWorkOrderLineId() {
        return workOrderLineId;
    }

    public void setWorkOrderLineId(Long workOrderLineId) {
        this.workOrderLineId = workOrderLineId;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Long getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Long openQuantity) {
        this.openQuantity = openQuantity;
    }

    public Long getAllocatingQuantity() {
        return allocatingQuantity;
    }

    public void setAllocatingQuantity(Long allocatingQuantity) {
        this.allocatingQuantity = allocatingQuantity;
    }
}
