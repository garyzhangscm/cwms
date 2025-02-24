package com.garyzhangscm.cwms.workorder.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Entity
@Table(name = "work_order_flow")
public class WorkOrderFlow extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_flow_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;


    @OneToMany(
            mappedBy = "workOrderFlow",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderFlowLine> lines = new ArrayList<>();

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


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<WorkOrderFlowLine> getLines() {
        return lines;
    }

    public void setLines(List<WorkOrderFlowLine> lines) {
        this.lines = lines;
    }
}
