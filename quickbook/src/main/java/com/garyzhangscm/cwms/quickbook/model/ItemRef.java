package com.garyzhangscm.cwms.quickbook.model;

import java.io.Serializable;

/**
 * Item ref in Invoice Line / Sales Item Line Detail,
 * refer to an item
 */
public class ItemRef implements Serializable {

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
