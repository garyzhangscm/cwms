package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Hardware that we are using to monitor the status of the production line
 */
@Entity
@Table(name = "production_line_monitor_transaction")
public class ProductionLineMonitorTransaction extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_line_monitor_transaction_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @ManyToOne
    @JoinColumn(name = "production_line_monitor_id")
    private ProductionLineMonitor productionLineMonitor;

    /**
     * A production line may have multiple monitor for different reasons
     */
    @ManyToOne
    @JoinColumn(name = "production_line_id")
    private ProductionLine productionLine;


    @Column(name = "cycle_time")
    private Double cycleTime;

    public ProductionLineMonitorTransaction(){}
    public ProductionLineMonitorTransaction(Long warehouseId, ProductionLineMonitor productionLineMonitor, ProductionLine productionLine, Double cycleTime) {
        this.warehouseId = warehouseId;
        this.productionLineMonitor = productionLineMonitor;
        this.productionLine = productionLine;
        this.cycleTime = cycleTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductionLineMonitor getProductionLineMonitor() {
        return productionLineMonitor;
    }

    public void setProductionLineMonitor(ProductionLineMonitor productionLineMonitor) {
        this.productionLineMonitor = productionLineMonitor;
    }

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
    }

    public Double getCycleTime() {
        return cycleTime;
    }

    public void setCycleTime(Double cycleTime) {
        this.cycleTime = cycleTime;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

}
