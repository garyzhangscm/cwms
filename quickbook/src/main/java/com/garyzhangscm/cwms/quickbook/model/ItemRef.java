package com.garyzhangscm.cwms.quickbook.model;

import java.io.Serializable;

/**
 * Item ref in Invoice Line / Sales Item Line Detail,
 * refer to an item
 */
public class ItemRef implements Serializable {

    // item ID
    private String value;

    // item name
    private String name;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
