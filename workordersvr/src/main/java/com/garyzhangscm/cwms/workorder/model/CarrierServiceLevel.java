package com.garyzhangscm.cwms.workorder.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CarrierServiceLevel implements Serializable {

    private Long id;

    private String name;

    private String description;

    private CarrierServiceLevelType type;

    private Carrier carrier;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CarrierServiceLevelType getType() {
        return type;
    }

    public void setType(CarrierServiceLevelType type) {
        this.type = type;
    }

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }
}
