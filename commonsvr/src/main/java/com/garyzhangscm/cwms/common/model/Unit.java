package com.garyzhangscm.cwms.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "unit")
public class Unit extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private UnitType type;


    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    // rate to the base unit of the same type
    @Column(name = "ratio")
    private Double ratio;


    // whether the unit is a base unit in the
    // type. All data of this type will be saved
    // in this unit
    @Column(name = "base_unit_flag")
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
