package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "work_order_reversed_production_inventory")
public class WorkOrderReverseProductionInventory extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_reversed_production_inventory_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_order_produce_transaction_id")
    @JsonIgnore
    private WorkOrderProduceTransaction workOrderProduceTransaction;



    @Column(name = "lpn")
    private String lpn;

    @Column(name = "quantity")
    private Long quantity;

    public WorkOrderReverseProductionInventory(){}

    public WorkOrderReverseProductionInventory(WorkOrderProduceTransaction workOrderProduceTransaction,
                                               String lpn,
                                               Long quantity){
        this.workOrderProduceTransaction = workOrderProduceTransaction;
        this.lpn = lpn;
        this.quantity = quantity;
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

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
}
