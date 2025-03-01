package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Entity
@Table(name = "work_order_complete_transaction")
public class WorkOrderCompleteTransaction extends AuditibleEntity<String>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_complete_transaction_id")
    @JsonProperty(value="id")
    private Long id;


    @OneToOne
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;



    @OneToMany(
            mappedBy = "workOrderCompleteTransaction",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderLineCompleteTransaction> workOrderLineCompleteTransactions = new ArrayList<>();


    @OneToMany(
            mappedBy = "workOrderProduceTransaction",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderKPITransaction> workOrderKPITransactions = new ArrayList<>();

    @OneToMany(
            mappedBy = "workOrderCompleteTransaction",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderByProductProduceTransaction> workOrderByProductProduceTransactions = new ArrayList<>();

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

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public List<WorkOrderLineCompleteTransaction> getWorkOrderLineCompleteTransactions() {
        return workOrderLineCompleteTransactions;
    }

    public void setWorkOrderLineCompleteTransactions(List<WorkOrderLineCompleteTransaction> workOrderLineCompleteTransactions) {
        this.workOrderLineCompleteTransactions = workOrderLineCompleteTransactions;
    }


    public List<WorkOrderByProductProduceTransaction> getWorkOrderByProductProduceTransactions() {
        return workOrderByProductProduceTransactions;
    }

    public void setWorkOrderByProductProduceTransactions(List<WorkOrderByProductProduceTransaction> workOrderByProductProduceTransactions) {
        this.workOrderByProductProduceTransactions = workOrderByProductProduceTransactions;
    }

    public List<WorkOrderKPITransaction> getWorkOrderKPITransactions() {
        return workOrderKPITransactions;
    }

    public void setWorkOrderKPITransactions(List<WorkOrderKPITransaction> workOrderKPITransactions) {
        this.workOrderKPITransactions = workOrderKPITransactions;
    }
}
