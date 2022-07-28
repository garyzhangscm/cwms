package com.garyzhangscm.cwms.quickbook.model;

import java.io.Serializable;

/**
 * Vendor ref in Purchase Order,
 * refer to a vendor
 */
public class VendorRef implements Serializable {

    // item ID
    private Long value;

    // item name
    private String name;

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
