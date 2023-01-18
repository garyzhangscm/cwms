package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = "work_order_labor")
public class WorkOrderLabor extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_labor_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "username")
    private String username;

    @ManyToOne
    @JoinColumn(name = "production_line_id")
    private ProductionLine productionLine;

    @Column(name = "last_check_in_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime lastCheckInTime;
    // @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    // @JsonSerialize(using = LocalDateTimeSerializer.class)
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    // private LocalDateTime lastCheckInTime;


    @Column(name = "last_check_out_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime lastCheckOutTime;
    // @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    // @JsonSerialize(using = LocalDateTimeSerializer.class)
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    // private LocalDateTime lastCheckOutTime;


    @Column(name = "labor_status")
    @Enumerated(EnumType.STRING)
    private WorkOrderLaborStatus workOrderLaborStatus;

    public WorkOrderLabor(){}
    public WorkOrderLabor(Long warehouseId, String username, ProductionLine productionLine,
                          ZonedDateTime lastCheckInTime, ZonedDateTime lastCheckOutTime, WorkOrderLaborStatus workOrderLaborStatus) {
        this.warehouseId = warehouseId;
        this.username = username;
        this.productionLine = productionLine;
        this.lastCheckInTime = lastCheckInTime;
        this.lastCheckOutTime = lastCheckOutTime;
        this.workOrderLaborStatus = workOrderLaborStatus;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ZonedDateTime getLastCheckInTime() {
        return lastCheckInTime;
    }

    public void setLastCheckInTime(ZonedDateTime lastCheckInTime) {
        this.lastCheckInTime = lastCheckInTime;
    }

    public ZonedDateTime getLastCheckOutTime() {
        return lastCheckOutTime;
    }

    public void setLastCheckOutTime(ZonedDateTime lastCheckOutTime) {
        this.lastCheckOutTime = lastCheckOutTime;
    }

    public WorkOrderLaborStatus getWorkOrderLaborStatus() {
        return workOrderLaborStatus;
    }

    public void setWorkOrderLaborStatus(WorkOrderLaborStatus workOrderLaborStatus) {
        this.workOrderLaborStatus = workOrderLaborStatus;
    }

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
