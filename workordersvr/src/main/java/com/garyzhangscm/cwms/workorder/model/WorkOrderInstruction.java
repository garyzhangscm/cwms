package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "work_order_instruction")
public class WorkOrderInstruction extends AuditibleEntity<String>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_instruction_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    @JsonIgnore
    private WorkOrder workOrder;

    @Column(name = "sequence")
    private Integer sequence;

    @Column(name = "instruction")
    private String instruction;

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

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getInstruction() {
        return instruction;
    }
}
