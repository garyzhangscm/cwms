package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    // if we want to consume from the final good of another work order
    // if we have this field setup, then we will automatically produce
    // the quantity from other work order and consume it for this work order
    // we will need to know the work order number, total quantity we would
    // like the second work order to be produced and from which production line
    // the quantity will be produced
    @ManyToOne
    @JoinColumn(name = "consume_from_work_order_id")
    private WorkOrder consumeFromWorkOrder;

    @Column(name = "consume_from_work_order_quantity")
    private Long consumeFromWorkOrderQuantity;

    @ManyToOne
    @JoinColumn(name = "consume_from_work_order_production_line_id")
    private ProductionLine consumeFromWorkOrderProductionLine;


    // if we want to consume LPNs that are not explicitly picked
    // for this work order

    @OneToMany(
            mappedBy = "workOrderLineConsumeTransaction",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<WorkOrderLineConsumeLPNTransaction> workOrderLineConsumeLPNTransactions = new ArrayList<>();



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

    public WorkOrder getConsumeFromWorkOrder() {
        return consumeFromWorkOrder;
    }

    public void setConsumeFromWorkOrder(WorkOrder consumeFromWorkOrder) {
        this.consumeFromWorkOrder = consumeFromWorkOrder;
    }

    public Long getConsumeFromWorkOrderQuantity() {
        return consumeFromWorkOrderQuantity;
    }

    public void setConsumeFromWorkOrderQuantity(Long consumeFromWorkOrderQuantity) {
        this.consumeFromWorkOrderQuantity = consumeFromWorkOrderQuantity;
    }

    public ProductionLine getConsumeFromWorkOrderProductionLine() {
        return consumeFromWorkOrderProductionLine;
    }

    public void setConsumeFromWorkOrderProductionLine(ProductionLine consumeFromWorkOrderProductionLine) {
        this.consumeFromWorkOrderProductionLine = consumeFromWorkOrderProductionLine;
    }

    public List<WorkOrderLineConsumeLPNTransaction> getWorkOrderLineConsumeLPNTransactions() {
        return workOrderLineConsumeLPNTransactions;
    }

    public void setWorkOrderLineConsumeLPNTransactions(List<WorkOrderLineConsumeLPNTransaction> workOrderLineConsumeLPNTransactions) {
        this.workOrderLineConsumeLPNTransactions = workOrderLineConsumeLPNTransactions;
    }
}
