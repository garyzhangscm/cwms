package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_line")
public class ProductionLine extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_line_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "inbound_stage_location_id")
    private Long inboundStageLocationId;

    @Transient
    private Location inboundStageLocation;

    @Column(name = "outbound_stage_location_id")
    private Long outboundStageLocationId;

    @Transient
    private Location outboundStageLocation;

    @Column(name = "production_line_location_id")
    private Long productionLineLocationId;

    @Transient
    private Location productionLineLocation;

    @OneToMany(mappedBy = "productionLine", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<WorkOrder> workOrders = new ArrayList<>();

    // Whether there's only one work order can be worked on
    // this production at any time
    @Column(name = "work_order_exclusive_flag")
    private Boolean workOrderExclusiveFlag = true;

    @Column(name = "enabled")
    private Boolean enabled = false;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Long getInboundStageLocationId() {
        return inboundStageLocationId;
    }

    public void setInboundStageLocationId(Long inboundStageLocationId) {
        this.inboundStageLocationId = inboundStageLocationId;
    }

    public Location getInboundStageLocation() {
        return inboundStageLocation;
    }

    public void setInboundStageLocation(Location inboundStageLocation) {
        this.inboundStageLocation = inboundStageLocation;
    }

    public Long getOutboundStageLocationId() {
        return outboundStageLocationId;
    }

    public void setOutboundStageLocationId(Long outboundStageLocationId) {
        this.outboundStageLocationId = outboundStageLocationId;
    }

    public Location getOutboundStageLocation() {
        return outboundStageLocation;
    }

    public void setOutboundStageLocation(Location outboundStageLocation) {
        this.outboundStageLocation = outboundStageLocation;
    }

    public Long getProductionLineLocationId() {
        return productionLineLocationId;
    }

    public void setProductionLineLocationId(Long productionLineLocationId) {
        this.productionLineLocationId = productionLineLocationId;
    }

    public Location getProductionLineLocation() {
        return productionLineLocation;
    }

    public void setProductionLineLocation(Location productionLineLocation) {
        this.productionLineLocation = productionLineLocation;
    }

    public List<WorkOrder> getWorkOrders() {
        return workOrders;
    }

    public void setWorkOrders(List<WorkOrder> workOrders) {
        this.workOrders = workOrders;
    }

    public Boolean getWorkOrderExclusiveFlag() {
        return workOrderExclusiveFlag;
    }

    public void setWorkOrderExclusiveFlag(Boolean workOrderExclusiveFlag) {
        this.workOrderExclusiveFlag = workOrderExclusiveFlag;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

}
