package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Entity
@Table(name = "master_production_schedule_line")
public class MasterProductionScheduleLine extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_production_schedule_line_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "master_production_schedule_id")
    @JsonIgnore
    private MasterProductionSchedule masterProductionSchedule;


    // planned quantity on the production line
    // may span across multiple days
    @Column(name = "planned_quantity")
    private Long quantity;

    // MPS line on the production line
    @ManyToOne
    @JoinColumn(name = "production_line_id")
    private ProductionLine productionLine;

    // planned quantity on different days
    @OneToMany(
            mappedBy = "masterProductionScheduleLine",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<MasterProductionScheduleLineDate> masterProductionScheduleLineDates = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MasterProductionSchedule getMasterProductionSchedule() {
        return masterProductionSchedule;
    }

    public void setMasterProductionSchedule(MasterProductionSchedule masterProductionSchedule) {
        this.masterProductionSchedule = masterProductionSchedule;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
    }

    public List<MasterProductionScheduleLineDate> getMasterProductionScheduleLineDates() {
        return masterProductionScheduleLineDates;
    }

    public void setMasterProductionScheduleLineDates(List<MasterProductionScheduleLineDate> masterProductionScheduleLineDates) {
        this.masterProductionScheduleLineDates = masterProductionScheduleLineDates;
    }
}
