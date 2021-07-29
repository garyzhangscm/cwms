package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order_produce_transaction")
public class WorkOrderProduceTransaction extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_produce_transaction_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @OneToMany(
            mappedBy = "workOrderProduceTransaction",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<WorkOrderLineConsumeTransaction> workOrderLineConsumeTransactions = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "production_line_id")
    private ProductionLine productionLine;

    @OneToMany(
            mappedBy = "workOrderProduceTransaction",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<WorkOrderProducedInventory> workOrderProducedInventories = new ArrayList<>();

    @OneToMany(
            mappedBy = "workOrderProduceTransaction",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<WorkOrderByProductProduceTransaction> workOrderByProductProduceTransactions = new ArrayList<>();


    @OneToMany(
            mappedBy = "workOrderProduceTransaction",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<WorkOrderKPITransaction> workOrderKPITransactions = new ArrayList<>();


    @Column(name = "consume_by_bom_quantity")
    private Boolean consumeByBomQuantity;

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

    public List<WorkOrderLineConsumeTransaction> getWorkOrderLineConsumeTransactions() {
        return workOrderLineConsumeTransactions;
    }

    public void setWorkOrderLineConsumeTransactions(List<WorkOrderLineConsumeTransaction> workOrderLineConsumeTransactions) {
        this.workOrderLineConsumeTransactions = workOrderLineConsumeTransactions;
    }

    public List<WorkOrderProducedInventory> getWorkOrderProducedInventories() {
        return workOrderProducedInventories;
    }

    public void setWorkOrderProducedInventories(List<WorkOrderProducedInventory> workOrderProducedInventories) {
        this.workOrderProducedInventories = workOrderProducedInventories;
    }

    public Boolean getConsumeByBomQuantity() {
        return consumeByBomQuantity;
    }

    public void setConsumeByBomQuantity(Boolean consumeByBomQuantity) {
        this.consumeByBomQuantity = consumeByBomQuantity;
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

    public ProductionLine getProductionLine() {
        return productionLine;
    }

    public void setProductionLine(ProductionLine productionLine) {
        this.productionLine = productionLine;
    }
}
