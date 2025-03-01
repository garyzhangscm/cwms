package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;

/**
 *
 */
@Entity
@Table(name = "work_order_flow_line")
public class WorkOrderFlowLine extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_flow_line_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "sequence")
    private Integer sequence;


    @ManyToOne
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;


    @ManyToOne
    @JoinColumn(name = "work_order_flow_id")
    @JsonIgnore
    private WorkOrderFlow workOrderFlow;

    public WorkOrderFlow getWorkOrderFlow() {
        return workOrderFlow;
    }

    public void setWorkOrderFlow(WorkOrderFlow workOrderFlow) {
        this.workOrderFlow = workOrderFlow;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public WorkOrderFlow getWorkOrderFow() {
        return workOrderFlow;
    }

    public void setWorkOrderFow(WorkOrderFlow workOrderFlow) {
        this.workOrderFlow = workOrderFlow;
    }
}
