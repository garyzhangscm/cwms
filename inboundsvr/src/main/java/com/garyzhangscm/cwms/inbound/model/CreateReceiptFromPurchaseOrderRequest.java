package com.garyzhangscm.cwms.inbound.model;

/**
 * A container to hold the create receipt from purchase order request
 * when the user want to crate a receipt from purchase order, the user will
 * only need to specify the quantity for each PO line. We will create a
 * receipt according to the matched PO line, along with the quantity specified
 * by the user
 */
public class CreateReceiptFromPurchaseOrderRequest {


    Long purchaseOrderLineId;
    Long quantity;

    public Long getPurchaseOrderLineId() {
        return purchaseOrderLineId;
    }

    public void setPurchaseOrderLineId(Long purchaseOrderLineId) {
        this.purchaseOrderLineId = purchaseOrderLineId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
}
