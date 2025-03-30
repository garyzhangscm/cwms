package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "work_order_produce_transaction")
public class WorkOrderProduceTransaction extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_produce_transaction_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @Transient
    private String workOrderNumber;

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
    List<WorkOrderReverseProductionInventory> workOrderReverseProductionInventories = new ArrayList<>();

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


    @ManyToOne
    @JoinColumn(name = "consume_by_bom_id")
    private BillOfMaterial consumeByBom;


    @Column(name = "reason_code_id")
    private Long reasonCodeId;

    @Column(name = "rf_code")
    private String rfCode;

    @Transient
    private ReasonCode reasonCode;


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkOrderProduceTransaction that = (WorkOrderProduceTransaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
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

    public BillOfMaterial getConsumeByBom() {
        return consumeByBom;
    }

    public void setConsumeByBom(BillOfMaterial consumeByBom) {
        this.consumeByBom = consumeByBom;
    }

    public List<WorkOrderReverseProductionInventory> getWorkOrderReverseProductionInventories() {
        return workOrderReverseProductionInventories;
    }

    public void setWorkOrderReverseProductionInventories(List<WorkOrderReverseProductionInventory> workOrderReverseProductionInventories) {
        this.workOrderReverseProductionInventories = workOrderReverseProductionInventories;
    }

    public void addWorkOrderReverseProductionInventories(WorkOrderReverseProductionInventory workOrderReverseProductionInventory) {
        this.workOrderReverseProductionInventories.add(workOrderReverseProductionInventory);
    }

    public Long getReasonCodeId() {
        return reasonCodeId;
    }

    public void setReasonCodeId(Long reasonCodeId) {
        this.reasonCodeId = reasonCodeId;
    }

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(ReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getRfCode() {
        return rfCode;
    }

    public void setRfCode(String rfCode) {
        this.rfCode = rfCode;
    }
}
