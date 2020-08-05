package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order_line_consume_transaction")
public class WorkOrderLineConsumeTransaction extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_line_consume_transaction_id")
    @JsonProperty(value="id")
    private Long id;


    @ManyToOne
    @JoinColumn(name = "work_order_produce_transaction_id")
    @JsonIgnore
    private WorkOrderProduceTransaction workOrderProduceTransaction;


    @ManyToOne
    @JoinColumn(name = "work_order_line_id")
    private WorkOrderLine workOrderLine;

    @Column(name = "consumed_quantity")
    private Long consumedQuantity;

    public WorkOrderLineConsumeTransaction() {}


    public WorkOrderLineConsumeTransaction(WorkOrderProduceTransaction workOrderProduceTransaction,
                                           WorkOrderLine workOrderLine,
                                           Long consumedQuantity) {

        this.workOrderProduceTransaction = workOrderProduceTransaction;
        this.workOrderLine = workOrderLine;
        this.consumedQuantity= consumedQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkOrderProduceTransaction getWorkOrderProduceTransaction() {
        return workOrderProduceTransaction;
    }

    public void setWorkOrderProduceTransaction(WorkOrderProduceTransaction workOrderProduceTransaction) {
        this.workOrderProduceTransaction = workOrderProduceTransaction;
    }

    public WorkOrderLine getWorkOrderLine() {
        return workOrderLine;
    }

    public void setWorkOrderLine(WorkOrderLine workOrderLine) {
        this.workOrderLine = workOrderLine;
    }

    public Long getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(Long consumedQuantity) {
        this.consumedQuantity = consumedQuantity;
    }
}
