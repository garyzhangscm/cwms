package com.garyzhangscm.cwms.outbound.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

public class Unit extends AuditibleEntity<String> {

    private Long id;

    private UnitType type;

    private String name;

    private String description;

    private Double ratio;

    private Boolean baseUnitFlag;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UnitType getType() {
        return type;
    }

    public void setType(UnitType type) {
        this.type = type;
    }

    public Double getRatio() {
        return ratio;
    }

    public void setRatio(Double ratio) {
        this.ratio = ratio;
    }

    public Boolean getBaseUnitFlag() {
        return baseUnitFlag;
    }

    public void setBaseUnitFlag(Boolean baseUnitFlag) {
        this.baseUnitFlag = baseUnitFlag;
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
}
