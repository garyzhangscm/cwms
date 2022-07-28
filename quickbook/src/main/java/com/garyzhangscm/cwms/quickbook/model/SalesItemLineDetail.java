package com.garyzhangscm.cwms.quickbook.model;

import java.io.Serializable;

public class SalesItemLineDetail implements Serializable {


    private ItemRef itemRef;

    private Double unitPrice;

    private Double qty;

    public ItemRef getItemRef() {
        return itemRef;
    }

    public void setItemRef(ItemRef itemRef) {
        this.itemRef = itemRef;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }
}
