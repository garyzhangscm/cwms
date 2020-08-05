package com.garyzhangscm.cwms.workorder.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

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
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<WorkOrderLineCompleteTransaction> workOrderLineCompleteTransactions = new ArrayList<>();


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
}
