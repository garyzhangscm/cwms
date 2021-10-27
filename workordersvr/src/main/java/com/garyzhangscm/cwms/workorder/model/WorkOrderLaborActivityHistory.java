package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.garyzhangscm.cwms.workorder.service.WorkOrderLaborService;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 *
 */
@Entity
@Table(name = "work_order_labor_activity_history")
public class WorkOrderLaborActivityHistory extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_labor_activity_history_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @ManyToOne
    @JoinColumn(name = "work_order_labor_id")
    private WorkOrderLabor workOrderLabor;


    @Column(name = "activity_type")
    @Enumerated(EnumType.STRING)
    private WorkOrderLaborActivityType activityType;


    // user who carry out this activity. May be different
    // from the labor's username
    @Column(name = "username")
    private String username;

    @Column(name = "original_value")
    private String originalValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "activity_time")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime activityTime;

    public WorkOrderLaborActivityHistory(){}
    public WorkOrderLaborActivityHistory(Long warehouseId, WorkOrderLabor workOrderLabor,
                                         WorkOrderLaborActivityType activityType,
                                         String originalValue, String newValue, LocalDateTime activityTime,
                                         String username) {
        this.warehouseId = warehouseId;
        this.workOrderLabor = workOrderLabor;
        this.activityType = activityType;
        this.originalValue = originalValue;
        this.newValue = newValue;
        this.activityTime = activityTime;
        this.username = username;
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

    public WorkOrderLabor getWorkOrderLabor() {
        return workOrderLabor;
    }

    public void setWorkOrderLabor(WorkOrderLabor workOrderLabor) {
        this.workOrderLabor = workOrderLabor;
    }

    public WorkOrderLaborActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(WorkOrderLaborActivityType activityType) {
        this.activityType = activityType;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public LocalDateTime getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(LocalDateTime activityTime) {
        this.activityTime = activityTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
