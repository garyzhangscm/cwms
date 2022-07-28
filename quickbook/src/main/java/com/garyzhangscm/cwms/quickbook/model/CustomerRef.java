package com.garyzhangscm.cwms.quickbook.model;

import java.io.Serializable;

/**
 * Customer ref in Invoice,
 * refer to a customer
 */
public class CustomerRef implements Serializable {

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
