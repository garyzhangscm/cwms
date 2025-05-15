package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order_line_complete_transaction")
public class WorkOrderLineCompleteTransaction extends AuditibleEntity<String> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_line_complete_transaction_id")
    @JsonProperty(value="id")
    private Long id;


    @ManyToOne
    @JoinColumn(name = "work_order_complete_transaction_id")
    @JsonIgnore
    private WorkOrderCompleteTransaction workOrderCompleteTransaction;


    @OneToMany(
            mappedBy = "workOrderLineCompleteTransaction",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<ReturnMaterialRequest> returnMaterialRequests = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "work_order_line_id")
    private WorkOrderLine workOrderLine;

    @Column(name = "adjusted_consumed_quantity")
    private Long adjustedConsumedQuantity;

    @Column(name = "scrapped_quantity")
    private Long scrappedQuantity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkOrderCompleteTransaction getWorkOrderCompleteTransaction() {
        return workOrderCompleteTransaction;
    }

    public void setWorkOrderCompleteTransaction(WorkOrderCompleteTransaction workOrderCompleteTransaction) {
        this.workOrderCompleteTransaction = workOrderCompleteTransaction;
    }

    public List<ReturnMaterialRequest> getReturnMaterialRequests() {
        return returnMaterialRequests;
    }

    public void setReturnMaterialRequests(List<ReturnMaterialRequest> returnMaterialRequests) {
        this.returnMaterialRequests = returnMaterialRequests;
    }

    public WorkOrderLine getWorkOrderLine() {
        return workOrderLine;
    }

    public void setWorkOrderLine(WorkOrderLine workOrderLine) {
        this.workOrderLine = workOrderLine;
    }

    public Long getAdjustedConsumedQuantity() {
        return adjustedConsumedQuantity;
    }

    public void setAdjustedConsumedQuantity(Long adjustedConsumedQuantity) {
        this.adjustedConsumedQuantity = adjustedConsumedQuantity;
    }

    public Long getScrappedQuantity() {
        return scrappedQuantity;
    }

    public void setScrappedQuantity(Long scrappedQuantity) {
        this.scrappedQuantity = scrappedQuantity;
    }
}
