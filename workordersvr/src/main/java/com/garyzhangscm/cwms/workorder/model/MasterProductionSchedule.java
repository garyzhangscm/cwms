package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = "master_production_schedule")
public class MasterProductionSchedule extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_production_schedule_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;

    @Column(name = "description")
    private String description;

    @Column(name = "cutoff_date")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime cutoffDate;


    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    // total quantity for the MPS. May span across multiple production line
    @Column(name = "total_quantity")
    private Long totalQuantity;


    @Transient
    private Long plannedQuantity;

    // one MPS line for each production line
    @OneToMany(
            mappedBy = "masterProductionSchedule",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<MasterProductionScheduleLine> masterProductionScheduleLines = new ArrayList<>();


    @Override
    public boolean equals(Object anotherMps) {
        if (this == anotherMps) {
            return true;
        }
        if (!(anotherMps instanceof MasterProductionSchedule)) {
            return false;
        }
        return this.getNumber().equals(((MasterProductionSchedule)anotherMps).getNumber());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public LocalDateTime getCutoffDate() {
        return cutoffDate;
    }

    public void setCutoffDate(LocalDateTime cutoffDate) {
        this.cutoffDate = cutoffDate;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public List<MasterProductionScheduleLine> getMasterProductionScheduleLines() {
        return masterProductionScheduleLines;
    }

    public void setMasterProductionScheduleLines(List<MasterProductionScheduleLine> masterProductionScheduleLines) {
        this.masterProductionScheduleLines = masterProductionScheduleLines;
    }

    public Long getPlannedQuantity() {
        if (Objects.nonNull(plannedQuantity)) {
            return plannedQuantity;
        }
        plannedQuantity = 0l;
        for (MasterProductionScheduleLine masterProductionScheduleLine : this.masterProductionScheduleLines) {
            for (MasterProductionScheduleLineDate masterProductionScheduleLineDate :
                    masterProductionScheduleLine.masterProductionScheduleLineDates) {

                plannedQuantity += masterProductionScheduleLineDate.getPlannedQuantity();
            }
        }
        return plannedQuantity;
    }

    public void setPlannedQuantity(Long plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }
}
