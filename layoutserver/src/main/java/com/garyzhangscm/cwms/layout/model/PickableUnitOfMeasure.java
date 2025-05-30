package com.garyzhangscm.cwms.layout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "pickable_unit_of_measure")
public class PickableUnitOfMeasure extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pickable_unit_of_measure_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "unit_of_measure_id")
    private Long unitOfMeasureId;

    @Transient
    private UnitOfMeasure unitOfMeasure;


    @ManyToOne
    @JoinColumn(name="location_group_id")
    @JsonIgnore
    private LocationGroup locationGroup;

    @ManyToOne
    @JoinColumn(name="pick_zone_id")
    @JsonIgnore
    private PickZone pickZone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(Long unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public LocationGroup getLocationGroup() {
        return locationGroup;
    }

    public void setLocationGroup(LocationGroup locationGroup) {
        this.locationGroup = locationGroup;
    }

    public PickZone getPickZone() {
        return pickZone;
    }

    public void setPickZone(PickZone pickZone) {
        this.pickZone = pickZone;
    }
}
