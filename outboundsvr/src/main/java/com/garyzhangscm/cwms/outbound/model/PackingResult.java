package com.garyzhangscm.cwms.outbound.model;

import java.io.Serializable;

public class PackingResult implements Serializable {
    private String itemName;
    private Long quantity;
    private Long packedQuantity;

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getPackedQuantity() {
        return packedQuantity;
    }

    public void setPackedQuantity(Long packedQuantity) {
        this.packedQuantity = packedQuantity;
    }
}
