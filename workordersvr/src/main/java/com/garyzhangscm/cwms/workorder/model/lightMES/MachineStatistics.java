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
    private ZonedDateTime startTime;

    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime endTime;

    private Long producedQuantity;
    private Long estimationQuantity;
    private double achievementRate;

    // if the item is an active assignment on the machine
    private boolean isActive;


    public MachineStatistics(){

    }


    public MachineStatistics(String itemName,
                             String workOrderNumber){

        this.itemName = itemName;
        this.workOrderNumber = workOrderNumber;
        this.producedQuantity = 0l;
        this.estimationQuantity = 0l;
        this.achievementRate = 0.0;
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

    public Long getProducedQuantity() {
        return producedQuantity;
    }

    public void setProducedQuantity(Long producedQuantity) {
        this.producedQuantity = producedQuantity;
    }



    public double getAchievementRate() {
        return achievementRate;
    }

    public void setAchievementRate(double achievementRate) {
        this.achievementRate = achievementRate;
    }

    public Long getEstimationQuantity() {
        return estimationQuantity;
    }

    public void setEstimationQuantity(Long estimationQuantity) {
        this.estimationQuantity = estimationQuantity;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
