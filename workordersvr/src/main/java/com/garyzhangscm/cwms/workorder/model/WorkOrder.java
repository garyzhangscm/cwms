package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = "work_order")
public class WorkOrder extends AuditibleEntity<String>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderLine> workOrderLines = new ArrayList<>();

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderInstruction> workOrderInstructions = new ArrayList<>();

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    List<WorkOrderKPI> workOrderKPIs = new ArrayList<>();

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    List<WorkOrderByProduct> workOrderByProducts = new ArrayList<>();

    /***
        @ManyToOne
        @JoinColumn(name = "production_line_id")
        private ProductionLine productionLine;

     We will switch to a 1 to Many relationship between
     work order and production line so that one work order
     can have multiple production line. Some warehouse will use
     ***/

    @OneToMany(mappedBy = "workOrder")
    @Where(clause = "deassigned = null or deassigned = false")
    private List<ProductionLineAssignment> productionLineAssignments = new ArrayList<>();

    @Column(name = "item_id")
    private Long itemId;


    // if the work order is created from short allocation
    @Column(name = "short_allocation_id")
    private Long shortAllocationId;


    // When the work order is created by
    // following a specific BOM
    @ManyToOne
    @JoinColumn(name = "bill_of_material_id")
    private BillOfMaterial billOfMaterial;

    @ManyToOne
    @JoinColumn(name = "work_order_flow_line_id")
    @JsonIgnore
    private WorkOrderFlowLine workOrderFlowLine;

    // When the work order is created from a specific
    // production plan
    @ManyToOne
    @JoinColumn(name = "production_plan_line_id")
    private ProductionPlanLine productionPlanLine;


    @Transient
    private Item item;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    // customer's PO number that
    // generate this work order
    @Column(name = "po_number")
    private String poNumber;

    @Transient
    private Warehouse warehouse;

    @Column(name = "expected_quantity")
    private Long expectedQuantity;

    @Column(name = "produced_quantity")
    private Long producedQuantity;


    @OneToMany(mappedBy = "workOrder")
    private List<WorkOrderAssignment> assignments = new ArrayList<>();

    @Column(name = "status")
    private WorkOrderStatus status;

    @Column(name = "consume_by_bom_only")
    private Boolean consumeByBomOnly;

    @ManyToOne
    @JoinColumn(name = "consume_by_bom_id")
    private BillOfMaterial consumeByBom;

    // whether we consume the material per transaction
    // or once when the whole work order is closed.
    // If we have value setup in the work order level, it will
    // override the one we setup in the work order configuration
    @Column(name = "material_consume_timing")
    @Enumerated(EnumType.STRING)
    private WorkOrderMaterialConsumeTiming materialConsumeTiming;


    // Some statistics numbers that we can show
    // in the frontend
    @Transient
    private Integer totalLineCount;
    @Transient
    private Integer totalItemCount;
    @Transient
    private Long totalExpectedQuantity;
    @Transient
    private Long totalOpenQuantity; // Open quantity that is not allocated yet
    @Transient
    private Long totalOpenPickQuantity;
    @Transient
    private Long totalPickedQuantity;


    // qc needed by quantity
    @Column(name = "qc_quantity")
    private Long qcQuantity = 0l;

    // qc needed by percentage
    @Column(name = "qc_percentage")
    private Double qcPercentage = 0.0;

    // quantity already QCed
    @Column(name = "qc_quantity_requested")
    private Long qcQuantityRequested = 0L;
    @Column(name = "qc_quantity_completed")
    private Long qcQuantityCompleted = 0L;

    // bind the work order to the customer's order
    // if this is a build to order type of work order
    @Column(name = "bto_outbound_order_id")
    private Long btoOutboundOrderId;
    @Column(name = "bto_customer_id")
    private Long btoCustomerId;




    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkOrder workOrder = (WorkOrder) o;
        if (Objects.equals(id, workOrder.id)) {
            return true;
        }
        return Objects.equals(number, workOrder.number) &&
                Objects.equals(warehouseId, workOrder.warehouseId);
    }

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
    public int hashCode() {
        return Objects.hash(id, number, warehouseId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public List<WorkOrderLine> getWorkOrderLines() {
        return workOrderLines;
    }

    public void setWorkOrderLines(List<WorkOrderLine> workOrderLines) {
        this.workOrderLines = workOrderLines;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Long getProducedQuantity() {
        return producedQuantity;
    }

    public void setProducedQuantity(Long producedQuantity) {
        this.producedQuantity = producedQuantity;
    }

    public List<WorkOrderInstruction> getWorkOrderInstructions() {
        return workOrderInstructions;
    }

    public void setWorkOrderInstructions(List<WorkOrderInstruction> workOrderInstructions) {
        this.workOrderInstructions = workOrderInstructions;
    }

    public List<ProductionLineAssignment> getProductionLineAssignments() {
        return productionLineAssignments;
    }

    public void setProductionLineAssignments(List<ProductionLineAssignment> productionLineAssignments) {
        this.productionLineAssignments = productionLineAssignments;
    }

    public List<WorkOrderAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<WorkOrderAssignment> assignments) {
        this.assignments = assignments;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public void setStatus(WorkOrderStatus status) {
        this.status = status;
    }

    public BillOfMaterial getBillOfMaterial() {
        return billOfMaterial;
    }

    public void setBillOfMaterial(BillOfMaterial billOfMaterial) {
        this.billOfMaterial = billOfMaterial;
    }

    public List<WorkOrderKPI> getWorkOrderKPIs() {
        return workOrderKPIs;
    }

    public void setWorkOrderKPIs(List<WorkOrderKPI> workOrderKPIs) {
        this.workOrderKPIs = workOrderKPIs;
    }

    public List<WorkOrderByProduct> getWorkOrderByProducts() {
        return workOrderByProducts;
    }

    public void setWorkOrderByProducts(List<WorkOrderByProduct> workOrderByProducts) {
        this.workOrderByProducts = workOrderByProducts;
    }

    public ProductionPlanLine getProductionPlanLine() {
        return productionPlanLine;
    }

    public void setProductionPlanLine(ProductionPlanLine productionPlanLine) {
        this.productionPlanLine = productionPlanLine;
    }

    public Integer getTotalLineCount() {
        return totalLineCount;
    }

    public void setTotalLineCount(Integer totalLineCount) {
        this.totalLineCount = totalLineCount;
    }

    public Integer getTotalItemCount() {
        return totalItemCount;
    }

    public void setTotalItemCount(Integer totalItemCount) {
        this.totalItemCount = totalItemCount;
    }

    public Long getTotalExpectedQuantity() {
        return totalExpectedQuantity;
    }

    public void setTotalExpectedQuantity(Long totalExpectedQuantity) {
        this.totalExpectedQuantity = totalExpectedQuantity;
    }

    public Long getTotalOpenQuantity() {
        return totalOpenQuantity;
    }

    public void setTotalOpenQuantity(Long totalOpenQuantity) {
        this.totalOpenQuantity = totalOpenQuantity;
    }

    public Long getTotalOpenPickQuantity() {
        return totalOpenPickQuantity;
    }

    public void setTotalOpenPickQuantity(Long totalOpenPickQuantity) {
        this.totalOpenPickQuantity = totalOpenPickQuantity;
    }

    public Long getTotalPickedQuantity() {
        return totalPickedQuantity;
    }

    public void setTotalPickedQuantity(Long totalPickedQuantity) {
        this.totalPickedQuantity = totalPickedQuantity;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public WorkOrderMaterialConsumeTiming getMaterialConsumeTiming() {
        return materialConsumeTiming;
    }

    public void setMaterialConsumeTiming(WorkOrderMaterialConsumeTiming materialConsumeTiming) {
        this.materialConsumeTiming = materialConsumeTiming;
    }

    public Boolean getConsumeByBomOnly() {
        return consumeByBomOnly;
    }

    public void setConsumeByBomOnly(Boolean consumeByBomOnly) {
        this.consumeByBomOnly = consumeByBomOnly;
    }

    public BillOfMaterial getConsumeByBom() {
        return consumeByBom;
    }

    public void setConsumeByBom(BillOfMaterial consumeByBom) {
        this.consumeByBom = consumeByBom;
    }

    public Long getQcQuantity() {
        return qcQuantity;
    }

    public void setQcQuantity(Long qcQuantity) {
        this.qcQuantity = qcQuantity;
    }

    public Double getQcPercentage() {
        return qcPercentage;
    }

    public void setQcPercentage(Double qcPercentage) {
        this.qcPercentage = qcPercentage;
    }

    public Long getQcQuantityRequested() {
        return qcQuantityRequested;
    }

    public void setQcQuantityRequested(Long qcQuantityRequested) {
        this.qcQuantityRequested = qcQuantityRequested;
    }

    public Long getQcQuantityCompleted() {
        return qcQuantityCompleted;
    }

    public void setQcQuantityCompleted(Long qcQuantityCompleted) {
        this.qcQuantityCompleted = qcQuantityCompleted;
    }

    public Long getBtoOutboundOrderId() {
        return btoOutboundOrderId;
    }

    public void setBtoOutboundOrderId(Long btoOutboundOrderId) {
        this.btoOutboundOrderId = btoOutboundOrderId;
    }

    public Long getBtoCustomerId() {
        return btoCustomerId;
    }

    public void setBtoCustomerId(Long btoCustomerId) {
        this.btoCustomerId = btoCustomerId;
    }

    public Long getShortAllocationId() {
        return shortAllocationId;
    }

    public void setShortAllocationId(Long shortAllocationId) {
        this.shortAllocationId = shortAllocationId;
    }

    public WorkOrderFlowLine getWorkOrderFlowLine() {
        return workOrderFlowLine;
    }

    public void setWorkOrderFlowLine(WorkOrderFlowLine workOrderFlowLine) {
        this.workOrderFlowLine = workOrderFlowLine;
    }
}
