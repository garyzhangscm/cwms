package com.garyzhangscm.cwms.quickbook.model;

import java.io.Serializable;

/**
 * Phone number
 */
public class Email implements Serializable {

    private Long id;

    private String address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
