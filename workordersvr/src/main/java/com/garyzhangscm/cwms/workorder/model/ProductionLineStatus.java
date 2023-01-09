package com.garyzhangscm.cwms.workorder.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.Column;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class ProductionLineStatus  {

    private ProductionLine productionLine;

    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime startTime;

    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime endTime;

    private boolean active;

    private double lastCycleTime;
    // total cycles in the time span
    private int totalCycles;

    private double averageCycleTime;



    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime lastCycleHappensTiming;

    public ProductionLineStatus(){}
    public ProductionLineStatus(ProductionLine productionLine, ZonedDateTime startTime, ZonedDateTime endTime,
                                boolean active, double lastCycleTime,
                                int totalCycles,
                                double averageCycleTime,
                                ZonedDateTime lastCycleHappensTiming) {
        this.productionLine = productionLine;
        this.startTime = startTime;
        this.endTime = endTime;
        this.active = active;
        this.lastCycleTime = lastCycleTime;
        this.averageCycleTime = averageCycleTime;
        this.lastCycleHappensTiming = lastCycleHappensTiming;
        this.totalCycles = totalCycles;
    }

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public double getLastCycleTime() {
        return lastCycleTime;
    }

    public void setLastCycleTime(double lastCycleTime) {
        this.lastCycleTime = lastCycleTime;
    }

    public double getAverageCycleTime() {
        return averageCycleTime;
    }

    public void setAverageCycleTime(double averageCycleTime) {
        this.averageCycleTime = averageCycleTime;
    }

    public ZonedDateTime getLastCycleHappensTiming() {
        return lastCycleHappensTiming;
    }

    public void setLastCycleHappensTiming(ZonedDateTime lastCycleHappensTiming) {
        this.lastCycleHappensTiming = lastCycleHappensTiming;
    }

    public int getTotalCycles() {
        return totalCycles;
    }

    public void setTotalCycles(int totalCycles) {
        this.totalCycles = totalCycles;
    }
}
