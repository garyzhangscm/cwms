package com.garyzhangscm.cwms.workorder.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;


@Entity
@Table(name = "production_shift_schedule")
public class ProductionShiftSchedule extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_shift_schedule_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    @Column(name = "shift_start_time")
    /**
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime shiftStartTime;
     **/
    private String shiftStartTime;

    @Column(name = "shift_end_time")
    /**
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime shiftEndTime;
    **/
    private String shiftEndTime;



    // whether shift end next day or the same day as
    // the start time
    @Column(name = "shift_end_next_day")
    private Boolean shiftEndNextDay;

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

    public String getShiftStartTime() {
        return shiftStartTime;
    }

    public void setShiftStartTime(String shiftStartTime) {
        this.shiftStartTime = shiftStartTime;
    }

    public String getShiftEndTime() {
        return shiftEndTime;
    }

    public void setShiftEndTime(String shiftEndTime) {
        this.shiftEndTime = shiftEndTime;
    }

    public Boolean getShiftEndNextDay() {
        return shiftEndNextDay;
    }

    public void setShiftEndNextDay(Boolean shiftEndNextDay) {
        this.shiftEndNextDay = shiftEndNextDay;
    }
}
