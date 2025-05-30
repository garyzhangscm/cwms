package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

import java.time.ZonedDateTime;

/**
 *
 */
@Entity
@Table(name = "master_production_schedule_line_date")
public class MasterProductionScheduleLineDate extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_production_schedule_line_date_id")
    @JsonProperty(value="id")
    private Long id;



    @ManyToOne
    @JoinColumn(name = "master_production_schedule_line_id")
    @JsonIgnore
    private MasterProductionScheduleLine masterProductionScheduleLine;


    // planned quantity on a specific date
    @Column(name = "planned_quantity")
    private Long plannedQuantity;

    @Column(name = "planned_date")
    // @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    // @JsonSerialize(using = LocalDateTimeSerializer.class)
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime plannedDate;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MasterProductionScheduleLine getMasterProductionScheduleLine() {
        return masterProductionScheduleLine;
    }

    public void setMasterProductionScheduleLine(MasterProductionScheduleLine masterProductionScheduleLine) {
        this.masterProductionScheduleLine = masterProductionScheduleLine;
    }

    public Long getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(Long plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }

    public ZonedDateTime getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(ZonedDateTime plannedDate) {
        this.plannedDate = plannedDate;
    }
}
