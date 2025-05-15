package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

@Entity
@Table(name = "work_order_line_consume_lpn_transaction")
public class WorkOrderLineConsumeLPNTransaction extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_line_consume_lpn_transaction_id")
    @JsonProperty(value="id")
    private Long id;


    @ManyToOne
    @JoinColumn(name = "work_order_line_consume_transaction_id")
    @JsonIgnore
    private WorkOrderLineConsumeTransaction workOrderLineConsumeTransaction;


    @Column(name = "lpn")
    private String lpn;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "consumed_quantity")
    private Long consumedQuantity;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkOrderLineConsumeTransaction getWorkOrderLineConsumeTransaction() {
        return workOrderLineConsumeTransaction;
    }

    public void setWorkOrderLineConsumeTransaction(WorkOrderLineConsumeTransaction workOrderLineConsumeTransaction) {
        this.workOrderLineConsumeTransaction = workOrderLineConsumeTransaction;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public Long getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(Long consumedQuantity) {
        this.consumedQuantity = consumedQuantity;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
}
