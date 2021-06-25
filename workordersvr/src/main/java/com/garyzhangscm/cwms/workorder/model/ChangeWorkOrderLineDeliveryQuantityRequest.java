package com.garyzhangscm.cwms.workorder.model;

public class ChangeWorkOrderLineDeliveryQuantityRequest {
    Long workOrderLineId;
    Long quantityBeingDelivered;
    Long deliveredLocationId;

    public ChangeWorkOrderLineDeliveryQuantityRequest(){}

    public ChangeWorkOrderLineDeliveryQuantityRequest(
            Long workOrderLineId,
            Long quantityBeingDelivered,
            Long deliveredLocationId) {
        this.workOrderLineId = workOrderLineId;
        this.quantityBeingDelivered = quantityBeingDelivered;
        this.deliveredLocationId = deliveredLocationId;
    }

    public Long getWorkOrderLineId() {
        return workOrderLineId;
    }

    public void setWorkOrderLineId(Long workOrderLineId) {
        this.workOrderLineId = workOrderLineId;
    }

    public Long getQuantityBeingDelivered() {
        return quantityBeingDelivered;
    }

    public void setQuantityBeingDelivered(Long quantityBeingDelivered) {
        this.quantityBeingDelivered = quantityBeingDelivered;
    }

    public Long getDeliveredLocationId() {
        return deliveredLocationId;
    }

    public void setDeliveredLocationId(Long deliveredLocationId) {
        this.deliveredLocationId = deliveredLocationId;
    }
}
