package com.garyzhangscm.cwms.workorder.model.lightMES;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.garyzhangscm.cwms.workorder.model.CustomZonedDateTimeDeserializer;
import com.garyzhangscm.cwms.workorder.model.CustomZonedDateTimeSerializer;
import com.garyzhangscm.cwms.workorder.model.ProductionLine;
import com.garyzhangscm.cwms.workorder.model.ProductionLineAssignment;

import java.time.ZonedDateTime;
import java.util.List;

public class MachineStatistics {


    private String itemName;
    private String workOrderNumber;

    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime shiftStartTime;

    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime shiftEndTime;

    private Long producedQuantity;
    private Long shiftEstimationQuantity;
    private double achievementRate;

    private int pulseCount;

    public MachineStatistics(){

    }


    public MachineStatistics(String itemName,
                             String workOrderNumber){

        this.itemName = itemName;
        this.workOrderNumber = workOrderNumber;
        this.producedQuantity = 0l;
        this.shiftEstimationQuantity = 0l;
        this.achievementRate = 0.0;
        this.pulseCount = 0;
    }


    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public ZonedDateTime getShiftStartTime() {
        return shiftStartTime;
    }

    public void setShiftStartTime(ZonedDateTime shiftStartTime) {
        this.shiftStartTime = shiftStartTime;
    }

    public ZonedDateTime getShiftEndTime() {
        return shiftEndTime;
    }

    public void setShiftEndTime(ZonedDateTime shiftEndTime) {
        this.shiftEndTime = shiftEndTime;
    }

    public Long getProducedQuantity() {
        return producedQuantity;
    }

    public void setProducedQuantity(Long producedQuantity) {
        this.producedQuantity = producedQuantity;
    }

    public int getPulseCount() {
        return pulseCount;
    }

    public void setPulseCount(int pulseCount) {
        this.pulseCount = pulseCount;
    }

    public double getAchievementRate() {
        return achievementRate;
    }

    public void setAchievementRate(double achievementRate) {
        this.achievementRate = achievementRate;
    }

    public Long getShiftEstimationQuantity() {
        return shiftEstimationQuantity;
    }

    public void setShiftEstimationQuantity(Long shiftEstimationQuantity) {
        this.shiftEstimationQuantity = shiftEstimationQuantity;
    }
}
