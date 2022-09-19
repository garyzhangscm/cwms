package com.garyzhangscm.cwms.workorder.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.Column;
import java.time.LocalDateTime;

public class ProductionLineStatus  {

    private ProductionLine productionLine;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime startTime;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime endTime;

    private boolean active;

    private double lastCycleTime;
    // total cycles in the time span
    private int totalCycles;

    private double averageCycleTime;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime lastCycleHappensTiming;

    public ProductionLineStatus(){}
    public ProductionLineStatus(ProductionLine productionLine, LocalDateTime startTime, LocalDateTime endTime,
                                boolean active, double lastCycleTime,
                                int totalCycles,
                                double averageCycleTime,
                                LocalDateTime lastCycleHappensTiming) {
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
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

    public LocalDateTime getLastCycleHappensTiming() {
        return lastCycleHappensTiming;
    }

    public void setLastCycleHappensTiming(LocalDateTime lastCycleHappensTiming) {
        this.lastCycleHappensTiming = lastCycleHappensTiming;
    }

    public int getTotalCycles() {
        return totalCycles;
    }

    public void setTotalCycles(int totalCycles) {
        this.totalCycles = totalCycles;
    }
}
