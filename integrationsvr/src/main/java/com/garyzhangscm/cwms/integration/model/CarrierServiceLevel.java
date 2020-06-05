package com.garyzhangscm.cwms.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

public class CarrierServiceLevel {

    private Long id;

    private String name;

    private String description;


    private CarrierServiceLevelType type;



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


}
